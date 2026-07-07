---
name: code
description: Execute the tasks in implementation_plan.md at the repository root, one at a time — implement each task per the plan, add or update JUnit tests achieving at least 80% coverage for the changed code, run the full test suite, verify the task introduced no new Checkstyle/PMD/SpotBugs issues, and check off the task's checkbox directly in implementation_plan.md before moving to the next. Runs autonomously — user intervention is limited to unresolved errors, decisions only the user can make, or permissions the allowed-tools list doesn't cover. Warns the user up front if implementation_plan.md already exists at the repository root (expected only when resuming an interrupted prior run). On successful completion, archives the plan to .archive/, asks the user whether to open follow-up tasks or GitHub issues if overall code quality isn't excellent, and asks for a final review of all code changes. Invoke as `/code` once implementation_plan.md exists (e.g. produced by the `plan` skill).
allowed-tools: Read Edit Write Bash(mvn *) Bash(git status *) Bash(git diff *) Bash(git log *) Bash(find *) Bash(grep *) Bash(ls *) Bash(mkdir *) Bash(mv *) Bash(date *) Bash(gh issue create *) TaskCreate TaskUpdate TaskList TaskGet Skill
---

# Implementation

Execute `implementation_plan.md` end to end: implement each listed task against the real codebase, back it with
tests that meet this repository's coverage bar, check it off in the plan file, and move on — with as little
back-and-forth with the user as possible. This skill writes code; it is the execution counterpart to the `plan`
skill. On successful completion it archives the plan file (Step 8) so the root only ever holds the plan for
work that is still outstanding.

## Step 1 — Load the plan, and warn if one already exists

Read `implementation_plan.md` at the repository root.

- **Missing or empty**: tell the user there is nothing to execute and suggest running `/plan` first. Stop — this
  is not a case to interrupt the user about, just report it.
- **Present**: warn the user before doing anything else — this is a heads-up, not a question, so don't block on
  it unless the file looks genuinely inconsistent with the current task. Because a successful run always
  archives the plan to `.archive/` (Step 8), finding `implementation_plan.md` at the root when a run starts
  should only mean one of:
  - It was just written by the `plan` skill for the task about to be executed (no checkboxes checked yet).
  - A previous `/code` run on this same task was interrupted before finishing (some checkboxes checked, some
    not).
  - Unusually, every checkbox is already checked — a prior run finished all tasks but was interrupted before it
    could archive the file. Say so explicitly, then archive it immediately per Step 8 and report there is
    nothing left to execute, rather than re-running already-completed work.
  State which of these applies based on the checkbox state you see, then continue treating the file as the plan
  to execute/resume. If the plan's content clearly describes unrelated or stale work (not the task currently
  being asked about), stop and confirm with the user instead of assuming.
- Otherwise, read the plan in full, including the "Task summary" and "Current code state" sections for context,
  and the numbered "Implementation steps" for the actual work list.

## Step 2 — Verify checkbox placeholders

The `plan` skill gives every top-level task and every sub-task its own empty markdown checkbox (`[ ]`). Track
completion by checking those boxes — do not introduce a separate status tag or notation.

- Confirm each task/sub-task line has a `[ ]`/`[x]` checkbox. If the plan predates this convention and is
  missing checkboxes on some lines, add `[ ]` to those lines now (don't guess completion state — treat anything
  without a checked box as not done) and save the file before continuing.
- Treat any task or sub-task whose checkbox is already `[x]` (from a prior run of this skill) as already done —
  this makes the skill resumable across interrupted runs.

## Step 3 — Track progress, and capture a project-wide quality baseline

Mirror the plan's top-level tasks into the session's task list (`TaskCreate`), one per pending task, so the user
can see live progress. Mark each `TaskUpdate`d as completed as you finish it, in step with the plan-file update
in Step 5. This is in addition to, not a replacement for, updating `implementation_plan.md` — the plan file is
the durable record; the task list is the live view.

Before implementing anything, run `Skill({skill: "java-code-quality"})` once, unscoped, and record the total
issue count per tool (Checkstyle/PMD/SpotBugs) plus the per-file list. This is the baseline the final check in
Step 7 compares against to judge whether the plan's work left overall code quality at least as good as it found
it. Skip this only if the `java-code-quality` skill is unavailable in this repository.

## Step 4 — Implement tasks one at a time, in plan order

Process top-level tasks strictly in the order they appear (the `plan` skill orders them so each is buildable on
top of the previous one). Skip any task whose checkbox is already checked (`[x]`). For each unchecked task:

1. **Re-check the current code state** for the files the task touches — the plan may have gone stale since it
   was written (other tasks just completed, or the file changed for unrelated reasons). Read the actual current
   content before editing; don't assume the plan's "Current code state" section is still accurate. Also run
   `java-code-quality` scoped to the file(s) this task is about to touch
   (`Skill({skill: "java-code-quality", args: "<ClassName>"})`, once per touched class), and record any issues
   already present as this task's pre-change baseline — an empty baseline if the file is new. This is what the
   quality check later in this task list compares against, so it flags only issues this task introduces, not
   pre-existing ones.
2. **Implement exactly what the task specifies**: the named file(s), the described class/method/field, the
   behavior, the hook/interface it implements. Follow this repository's conventions from `CLAUDE.md` (Java 17,
   `final var` for locals, full Javadoc with `@param`/`@return`/`@throws` on every public/protected member,
   `IllegalArgumentException` for invalid arguments, no new runtime dependencies). Don't add anything the task
   didn't ask for — no speculative abstractions, no unrelated cleanup.
3. **Write or update the tests** the task calls for, following the existing test style in the same package
   (JUnit 5, Mockito only where already used). Cover the new/changed behavior, including edge cases implied by
   the Javadoc `@throws` contracts (e.g. null/invalid-argument cases).
4. **Run the scoped tests** for the affected class(es) via the `java-test` skill, scoped to the touched test
   class(es)/method(s): `Skill({skill: "java-test", args: "<TestClass>"})` (or a comma/wildcard selector per
   that skill's Step 1 if several classes are affected). If the `java-test` skill is unavailable in this
   repository, fall back to `mvn test -Dtest=<TestClass>` directly. Fix and re-run until green — don't move on
   with a red test.
5. **Check coverage** for the changed/new classes is at least 80% line coverage, via the `java-coverage` skill:
   `Skill({skill: "java-coverage", args: "<TestClass>"})` (or a comma/wildcard selector per that skill's Step 1,
   plus explicit target class names, if the task spans several classes). If the `java-coverage` skill is
   unavailable in this repository, fall back to running
   `mvn clean jacoco:prepare-agent test jacoco:report -Dtest=<TestClass>` directly and reading the generated
   `target/site/jacoco/jacoco.csv` for the specific class(es) touched — check the actual reported percentage,
   don't estimate it. If under 80%, add tests for the uncovered branches/lines and re-check.
6. **Run the full suite** (`mvn clean test`) before marking the task done, to catch regressions in other classes
   this task's change may have affected.
7. **Check code quality for the touched file(s)** via `java-code-quality`, scoped the same way as item 1:
   `Skill({skill: "java-code-quality", args: "<ClassName>"})`. Diff the reported issues against this task's
   baseline from item 1. Any issue that wasn't in the baseline is a regression this task introduced — fix it
   (then re-run 4–6 to confirm the fix didn't break tests or coverage) and re-check until none remain. Leave a
   new issue in place only if fixing it is genuinely unavoidable (e.g. it would contradict what the task
   explicitly specifies) — in that case say so, and why, in the Step 5 note rather than silently accepting it.
   If the `java-code-quality` skill is unavailable in this repository, skip this item.
8. Only once 4–7 all pass: proceed to Step 5 for this task, then continue to the next unchecked task.

## Step 5 — Check the task's boxes in `implementation_plan.md`

Immediately after a task clears Step 4 (don't batch this until the end — the file must reflect true progress at
all times in case the run is interrupted):

- Check that task's own checkbox (`[ ]` → `[x]`), and the checkbox of every sub-task under it that was part of
  the completed work.
- Add a short note under the task (one or two lines) naming the files touched, the tests added/updated, the
  coverage achieved, and the code-quality result, e.g. `- [x] **Add `ComparableCollectionItemChangeDetector`** —
  tests in ComparableCollectionItemChangeDetectorTest (94% line coverage, no new Checkstyle/PMD/SpotBugs
  issues).` If a new issue was left in place as unavoidable (Step 4 item 7), name it and the reason here instead.
- Save the file, then update the mirrored entry in the session task list (Step 3) to completed.

## Step 6 — When to interrupt the user

Keep interruptions rare — by design, this skill should run start-to-finish unattended on a well-formed plan.
Stop and use `AskUserQuestion` (or plain text if no real choice is being offered) only when:

- A test keeps failing after a genuine attempt to fix it (implementation and/or test), and the failure suggests
  the plan's described approach is actually wrong or infeasible against the real code — not just a typo you can
  fix yourself.
- The plan is ambiguous in a way that changes correctness or scope and can't be safely inferred — conflicting
  instructions, a choice only the user can make (e.g. which of two APIs to break), or a missing decision. Mirror
  the bar used by the `plan` skill's Step 3: don't ask about things with an obvious best-practice answer.
- A tool call needs a permission outside this skill's `allowed-tools` list and the underlying action is risky or
  irreversible (e.g. anything beyond the scoped `mvn`/read-only `git` commands this skill is pre-approved for) —
  in this case the harness will already prompt; just make the case for why it's needed if asked.
- `implementation_plan.md` already has every checkbox checked when a run starts — handled in Step 1, not here;
  don't redo finished work.

Do not stop merely because a task is nontrivial — implement it. Do not check a task's box to move past a
blocker; leave it unchecked and surface the blocker instead (and skip Step 8's archiving — an incomplete plan
stays at the root so the next run can resume it).

## Step 7 — Final verification

Once every checkbox is checked, run the full local verification from `CLAUDE.md`:
`mvn clean jacoco:prepare-agent install jacoco:report javadoc:jar source:jar -P '!build-extras'` to confirm the
whole build — not just the incrementally-tested pieces — is healthy. If this fails, fix it (or, if it's a
genuine blocker per Step 6, stop and surface it) — do not archive a plan behind a broken build.

Then assess overall code quality against the baseline captured in Step 3:

1. Run `Skill({skill: "java-code-quality"})` unscoped, project-wide, and compare its total issue counts
   (Checkstyle/PMD/SpotBugs) and per-file list against the Step 3 baseline.
2. If the total issue count did not increase and is zero (or was already zero at baseline and still is), overall
   quality is excellent — proceed to Step 8.
3. Otherwise — the plan's work leaves the project with more issues than it started with, or a nonzero count
   persists — do not silently proceed. Use `AskUserQuestion` to tell the user what remains (counts and a short
   summary per tool) and ask whether they want to:
   - add follow-up task(s) to `implementation_plan.md` to fix the remaining issues before this run archives it
     (if chosen, add the task(s) with empty checkboxes and return to Step 4 to implement them — do not archive
     yet), or
   - file the remaining issues as new GitHub issues instead (`gh issue create`, one issue per distinct problem
     or logical group, with enough detail — file, line, tool, message — to act on later), or
   - leave it as-is and proceed to archive without further action.
   Respect whichever the user picks; only proceed to Step 8 once this choice has been acted on (issues filed, or
   explicit acknowledgment to proceed as-is).
4. If the `java-code-quality` skill is unavailable in this repository, skip this quality assessment and proceed
   to Step 8 directly.

## Step 8 — Archive the plan and ask for review

Only after Step 7 passes clean, with every task's checkbox checked, and the quality gate above resolved:

1. Determine an identifier for the archived filename:
   - If the plan was grounded in a GitHub issue (check the "Task summary" section and the plan's origin for an
     issue reference, e.g. "issue #42"), use that issue id.
   - Otherwise, use the current timestamp: `date +%Y%m%d%H%M%S`.
2. Create the archive directory if it doesn't exist yet (`mkdir -p .archive`), then move the plan there, renamed
   to embed that identifier: `mv implementation_plan.md .archive/implementation_plan_<issue_id>.md` (e.g.
   `.archive/implementation_plan_42.md` or `.archive/implementation_plan_20260707153000.md`). The repository
   root must no longer have an `implementation_plan.md` once this step completes successfully.
3. Summarize for the user: which tasks were completed, the files touched overall, the coverage achieved per
   task/class, the final code-quality outcome (excellent / follow-up filed / issues left as-is per Step 7), and
   where the plan was archived to.
4. Explicitly ask the user to review the code changes (e.g. `git diff` / `git status`) before anything is
   committed. Do not commit, push, or open a PR — this skill's job ends at working, tested, reviewable code.
