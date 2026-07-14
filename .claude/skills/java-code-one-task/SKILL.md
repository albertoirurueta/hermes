---
name: java-code-one-task
description: Implement a single task from a Java `implementation_plan.md`-style task list end-to-end — implementation, JUnit tests, license headers, Javadoc, a scoped test run, an 80% coverage check, a full-suite run, and a code-quality regression check against a pre-change baseline it captures itself — then hand back a short summary (files touched, tests added/updated, coverage achieved, code-quality outcome, and any blocker) for the caller to record. Does not read or write `implementation_plan.md`, check any checkbox, or track a session task list — that bookkeeping is the caller's job. Invoke as `/java-code-one-task <task description>`, passing the task's own text (what to build, the named file(s)/class(es)/method(s), the described behavior, and any sub-tasks) as the argument. Used by the `code` skill, which invokes this once per top-level plan task, in order, and checks the task's box in `implementation_plan.md` on a clean report.
allowed-tools: Read Edit Write Bash(mvn *) Bash(git status *) Bash(git diff *) Bash(git log *) Bash(find *) Bash(grep *) Bash(ls *) Skill Agent
---

# Implement one plan task

Carry out a single task end to end against the real codebase: implement it, back it with tests that meet this
repository's coverage bar, keep license headers and Javadoc current, verify nothing regressed, and report the
outcome — with as little back-and-forth with the user as possible. This is the execution unit the `code` skill
calls once per top-level task in `implementation_plan.md`; it never touches that file itself.

## Step 1 — Re-check the current code state and capture a pre-change quality baseline

Read the actual current content of the file(s) the task touches before editing — don't assume any "current code
state" notes passed in with the task are still accurate; other tasks may have just completed, or the file may
have changed for unrelated reasons. Also capture this task's pre-change quality baseline by delegating to a
sub-agent, once per touched class: `Agent({description: "Capture pre-change quality baseline for <ClassName>",
prompt: "Invoke Skill({skill: \"java-code-quality\", args: \"<ClassName>\"}), then report back only the list of
issues found for this class — not the raw generated reports."})`. Running this in a sub-agent and having it
return just the issue list (not the full Checkstyle/PMD/SpotBugs reports) keeps unneeded report content out of
the main context window. Record the returned issues as this task's pre-change baseline — an empty baseline if
the file is new. This is what the quality check in Step 9 compares against, so it flags only issues this task
introduces, not pre-existing ones. Skip the baseline capture (but not the code-state re-check) if the
`java-code-quality` skill is unavailable in this repository.

## Step 2 — Implement exactly what the task specifies

The named file(s), the described class/method/field, the behavior, the hook/interface it implements. Follow this
repository's conventions from `CLAUDE.md` (Java version, `final`/`var` type inference for locals (only if code
already uses it), full Javadoc with `@param`/`@return`/`@throws` on every public/protected member,
`IllegalArgumentException` for invalid arguments, no new runtime dependencies). Don't add anything the task
didn't ask for — no speculative abstractions, no unrelated cleanup.

## Step 3 — Write or update the tests

Follow the existing test style in the same package (JUnit 5, Mockito only where already used). Cover the
new/changed behavior, including edge cases implied by the Javadoc `@throws` contracts (e.g. null/invalid-argument
cases).

## Step 4 — Add license headers

To the file(s) this task added or modified, by delegating to a sub-agent rather than running `check-license`
directly: `Agent({description: "Add license headers for <ClassName>", prompt: "Invoke Skill({skill:
\"check-license\", args: \"<file1,file2,...>\"}) scoped to the file(s) this task added or modified. Report back
only which files were missing a header vs. fixed vs. already compliant, and — if no header convention existed
anywhere in the repo — whether the user chose to skip or generate one, and what was ultimately accepted. Not the
full audit output."})`. Running this in a sub-agent's separate context keeps the audit details out of the main
context window since only the outcome matters here. If the sub-agent reports the user chose to skip header
generation, respect that choice and say so in your Step 11 summary so the caller can carry it forward to later
tasks. If the `check-license` skill is unavailable in this repository, skip this item.

## Step 5 — Update Javadoc

For the file(s) this task added or modified, by delegating to a sub-agent rather than running `java-javadoc`
directly: `Agent({description: "Update Javadoc for <ClassName>", prompt: "Invoke Skill({skill: \"java-javadoc\",
args: \"<ClassName1,ClassName2,...>\"}) scoped to the class(es) this task added or modified. Report back only
whether Javadoc was added/updated and for which members, and whether the Javadoc build verification passed — not
the full audit output."})`. Running this in a sub-agent's separate context keeps the audit details out of the
main context window since only the outcome matters here. If the sub-agent reports the Javadoc build failed, fix
the reported issues and re-invoke until it reports success.

## Step 6 — Run the scoped tests

For the affected class(es) by delegating to a sub-agent rather than running `java-test` directly:
`Agent({description: "Run tests for <TestClass>", prompt: "Invoke Skill({skill: \"java-test\", args:
\"<TestClass>\"}) (or a comma/wildcard selector per that skill's Step 1 if several classes are affected; fall
back to `mvn test -Dtest=<TestClass>` directly if the java-test skill is unavailable). If everything passes,
report back only that all tests passed. If anything fails, report back only the failing test names, the failure
reason, and the stack trace for each — not the full Surefire output."})`. Running this in a sub-agent's separate
context keeps unneeded passing-test output out of the main context window. If the sub-agent reports failures, fix
the implementation and/or tests based on the reported names/reasons/stack traces, then re-invoke the same
sub-agent pattern — repeat until it reports all tests passed. Don't move on with a red test. If a failure
persists after a genuine fix attempt and suggests the task's described approach is actually wrong or infeasible
against the real code, stop here and follow Step 10 instead of continuing to force it.

## Step 7 — Check coverage

For the changed/new classes is at least 80% line coverage, by delegating to a sub-agent rather than running
`java-coverage` directly: `Agent({description: "Check coverage for <TestClass>", prompt: "Invoke Skill({skill:
\"java-coverage\", args: \"<TestClass>\"}) (or a comma/wildcard selector per that skill's Step 1, plus explicit
target class names, if several classes are involved; fall back to `mvn clean jacoco:prepare-agent test
jacoco:report -Dtest=<TestClass>` directly and reading `target/site/jacoco/jacoco.csv` if the java-coverage skill
is unavailable). Report back, for each target class only, its line coverage percentage and branch coverage
percentage — not the full report or which specific lines/branches are covered or uncovered."})`. Running this in
a sub-agent's separate context keeps the detailed per-line/per-branch report data out of the main context window
since only the percentages are needed here. Check the actual reported percentage, don't estimate it. If under
80%, add tests for the uncovered branches/lines (re-invoking the sub-agent from Step 6, and this one without a
sub-agent, to confirm) and re-check.

## Step 8 — Run the full suite

Before reporting this task done, to catch regressions in other classes this task's change may have affected, by
delegating to a sub-agent for the same reason as Step 6: `Agent({description: "Run full test suite", prompt: "Run
`mvn clean test`. If everything passes, report back only that all tests passed. If anything fails, report back
only the failing test names, the failure reason, and the stack trace for each — not the full Surefire
output."})`. Running this in a sub-agent's separate context keeps unneeded passing-test output out of the main
context window. If the sub-agent reports failures, fix the regression, then re-invoke the same sub-agent pattern
— repeat until it reports all tests passed.

## Step 9 — Check code quality for the touched file(s)

Scoped the same way as Step 1, by delegating to a sub-agent for the same reason as before:
`Agent({description: "Check for new quality issues in <ClassName>", prompt: "Invoke Skill({skill:
\"java-code-quality\", args: \"<ClassName>\"}). Compare the reported issues against this pre-change baseline:
<baseline from Step 1>. Report back only the issues that are newly appearing (not present in the baseline) — not
the full reports and not issues that were already present."})`. Running this in a sub-agent's separate context,
and having it do the diffing itself, keeps the full reports and already-known pre-existing issues out of the
main context window since only newly introduced issues matter here. Any issue reported back is a regression this
task introduced — fix it (then re-run Steps 6–8 to confirm the fix didn't break tests or coverage) and re-check
until none remain. Leave a new issue in place only if fixing it is genuinely unavoidable (e.g. it would
contradict what the task explicitly specifies) — in that case say so, and why, in your Step 11 summary rather
than silently accepting it. If the `java-code-quality` skill is unavailable in this repository, skip this item.

## Step 10 — When to interrupt the user

Keep interruptions rare — most tasks should complete unattended. Stop and use `AskUserQuestion` (or plain text if
no real choice is being offered) only when:

- A test keeps failing after a genuine attempt to fix it (implementation and/or test), and the failure suggests
  the task's described approach is actually wrong or infeasible against the real code — not just a typo you can
  fix yourself.
- The task is ambiguous in a way that changes correctness or scope and can't be safely inferred — conflicting
  instructions, a choice only the user can make (e.g. which of two APIs to break), or a missing decision. Don't
  ask about things with an obvious best-practice answer.

Do not stop merely because the task is nontrivial — implement it. Do not report the task done to work around a
blocker; report it as blocked instead (Step 11), with enough detail — what was tried, what failed, why — for the
caller to surface it to the user.

## Step 11 — Report the outcome

Once Steps 2–9 all pass (or Step 10 stopped things short), hand control back to the caller with a short summary:
the file(s) touched, the tests added/updated, the coverage achieved, and the code-quality result — including
whether a new issue was left in place as unavoidable and why, whether license-header generation was skipped by
the user, or whether the run stopped on a blocker per Step 10. This is what the caller (e.g. the `code` skill's
Step 5) records against the task in its own tracking; this skill itself never edits `implementation_plan.md`.
