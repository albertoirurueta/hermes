---
name: code
description: Execute the tasks in implementation_plan.md at the repository root, one at a time — implement each task per the plan, add or update tests achieving at least 80% coverage for the changed code, run the full test suite, verify the task introduced no new static-analysis issues, and check off the task's checkbox directly in implementation_plan.md before moving to the next. Dispatches each task to the language/framework-specific `<key>-code-one-task` skill declared by the plan (e.g. `java-code-one-task`, `dotnet-code-one-task`) and checks code quality via the matching `<key>-code-quality` skill (e.g. `java-code-quality`'s Checkstyle/PMD/SpotBugs, `dotnet-code-quality`'s StyleCop/CA rules) — falling back to implementing a task directly when the plan names no matching key. Runs autonomously — user intervention is limited to unresolved errors, decisions only the user can make, or permissions the allowed-tools list doesn't cover. Warns the user up front if implementation_plan.md already exists at the repository root (expected only when resuming an interrupted prior run). On successful completion, updates the project's Antora documentation (via the `update-docs` skill) to reflect all the changes the plan made, archives the plan to .archive/, asks the user whether to open follow-up tasks or GitHub issues if overall code quality isn't excellent, and asks for a final review of all code and documentation changes. Invoke as `/code` once implementation_plan.md exists (e.g. produced by the `plan` skill).
allowed-tools: Read Edit Write Bash(mvn *) Bash(detect-secrets *) Bash(git status *) Bash(git diff *) Bash(git log *) Bash(find *) Bash(grep *) Bash(ls *) Bash(mkdir *) Bash(mv *) Bash(date *) Bash(gh issue create *) TaskCreate TaskUpdate TaskList TaskGet Skill Agent
---

# Implementation

Execute `implementation_plan.md` end to end: implement each listed task against the real codebase, back it with
tests that meet this repository's coverage bar, check it off in the plan file, and move on — with as little
back-and-forth with the user as possible. This skill writes code; it is the execution counterpart to the `plan`
skill. On successful completion it brings the project's documentation in line with everything the plan changed
(Step 8) and archives the plan file (Step 9) so the root only ever holds the plan for work that is still
outstanding.

## Step 1 — Load the plan, and warn if one already exists

Read `implementation_plan.md` at the repository root.

- **Missing or empty**: tell the user there is nothing to execute and suggest running `/plan` first. Stop — this
  is not a case to interrupt the user about, just report it.
- **Present**: warn the user before doing anything else — this is a heads-up, not a question, so don't block on
  it unless the file looks genuinely inconsistent with the current task. Because a successful run always
  archives the plan to `.archive/` (Step 9), finding `implementation_plan.md` at the root when a run starts
  should only mean one of:
  - It was just written by the `plan` skill for the task about to be executed (no checkboxes checked yet).
  - A previous `/code` run on this same task was interrupted before finishing (some checkboxes checked, some
    not).
  - Unusually, every checkbox is already checked — a prior run finished all tasks but was interrupted before it
    could archive the file. Say so explicitly, then archive it immediately per Step 9 and report there is
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

## Step 3 — Track progress, and capture project-wide quality and security baselines

Mirror the plan's top-level tasks into the session's task list (`TaskCreate`), one per pending task, so the user
can see live progress. Mark each `TaskUpdate`d as completed as you finish it, in step with the plan-file update
in Step 5. This is in addition to, not a replacement for, updating `implementation_plan.md` — the plan file is
the durable record; the task list is the live view.

Before implementing anything, determine which language/framework(s) this plan is in play for: read the "Task
summary" section's **Language/framework** line if the plan declared one key for the whole plan, or the per-task
tags in "Implementation steps" if it declared them per task instead (this is exactly what the `plan` skill's Step
4/5 produce). Collect the distinct set of keys found across the whole plan — usually just one (e.g. `java` or
`dotnet`), occasionally more for a plan spanning several languages.

For each distinct key, capture a project-wide quality baseline by delegating to a sub-agent rather than running
that language's quality skill directly — use the matching `<key>-code-quality` skill for the detected language/
framework, e.g. `java-code-quality` when the plan is Java (Checkstyle/PMD/SpotBugs), or `dotnet-code-quality` when
it's .NET (StyleCop.Analyzers/CA rules), or whichever other `<key>-code-quality` skill matches a key found in this
repository's `.claude/skills`: `Agent({description: "Capture code-quality baseline (<key>)", prompt: "Invoke
Skill({skill: \"<key>-code-quality\"}) unscoped, then report back only the total issue count per tool and the
per-file list of issues — not the raw generated reports."})`. Running it in a sub-agent's separate context, and
having it return just the issue list rather than the full generated reports, keeps the report contents out of the
main context window since they aren't needed here — only the resulting list of issues is. Record each returned
list, keyed by its language/framework, as that key's baseline — the final check in Step 7 compares each one back
against its own key's baseline to judge whether the plan's work left overall code quality at least as good as it
found it. Skip capturing a baseline for any key that has no matching `<key>-code-quality` skill installed in this
repository, and skip this baseline capture entirely if no language/framework key could be determined for the plan
at all.

Also capture a project-wide security baseline the same way, by delegating to a sub-agent rather than running
`check-security` directly: `Agent({description: "Capture security baseline", prompt: "Invoke Skill({skill:
\"check-security\"}) unscoped, project-wide. Report back only the count and the per-file list of flagged secrets
(new since the last `.secrets.baseline` update, plus any still lacking a triage label) — not the raw
detect-secrets output or baseline file contents."})`. As with the quality baseline, running this in a sub-agent's
separate context keeps the raw scan output out of the main context window since only the resulting issue count/
list is needed here. Record that returned count/list as the baseline the final check in Step 7 compares against
— the goal is to catch any task in this run that introduces a new secret into the codebase, not to fix
pre-existing findings this run didn't cause. If the sub-agent reports that `detect-secrets` could not be
installed and the user chose to continue without scanning, record that security scanning is unavailable for this
run, skip this and the corresponding Step 7 security check entirely, and say so plainly in the Step 9 summary
rather than silently omitting it. Skip this baseline capture outright if the `check-security` skill is
unavailable in this repository.

## Step 4 — Implement tasks one at a time, in plan order

Process top-level tasks strictly in the order they appear (the `plan` skill orders them so each is buildable on
top of the previous one). Skip any task whose checkbox is already checked (`[x]`). For each unchecked task,
resolve which `<key>-code-one-task` skill it dispatches to before invoking anything:

1. If the task's own line carries an explicit language/framework tag (e.g. `_(dotnet)_`, per the `plan` skill's
   per-task tagging), use that key.
2. Otherwise, if the plan declared one **Language/framework** key for the whole plan in its "Task summary"
   section, use that key — it applies to every untagged task.
3. Otherwise (no per-task tag and no plan-wide key), the task's language/framework is unknown or unsupported.
   There is no `<key>-code-one-task` skill to delegate to — implement the task directly yourself instead,
   following the same obligations a one-task skill would (implement exactly what's specified, write/update
   tests, add license headers if this repository has a convention for them, keep documentation current, run the
   relevant tests and check coverage, and check for new code-quality issues), using this repository's own
   conventions from `CLAUDE.md`. Then continue to Step 5 for this task as usual.

Once a key is resolved (steps 1 or 2 above) and a matching `<key>-code-one-task` skill exists in this repository
(e.g. `java-code-one-task` for `java`, `dotnet-code-one-task` for `dotnet`), invoke `Skill({skill:
"<key>-code-one-task", args: "<the task's full text from the plan, including its sub-tasks and any relevant
"Current code state" context>"})`. That skill re-checks the current code state, implements the task, writes/
updates its tests, adds license headers, updates documentation (Javadoc, XML doc comments, or whatever this
language's equivalent is), runs the scoped tests, checks coverage, runs the full suite, and checks for new
code-quality issues against a pre-change baseline it captures itself — then hands back a short summary (files
touched, tests added/updated, coverage achieved, and the code-quality outcome, including any issue left in place
as unavoidable and why, or whether license-header generation was skipped by the user). If the resolved key has no
matching `<key>-code-one-task` skill installed in this repository, fall back to implementing the task directly,
same as case 3 above.

If the invoked `<key>-code-one-task` skill reports that it stopped short on a blocker (a failing test that
suggests the plan's approach itself is wrong, or an ambiguity only the user can resolve — see that skill's own
interrupt step), treat it the same as if it happened here: don't check the task's box, surface the blocker to the
user via `AskUserQuestion`, and stop rather than moving to the next task. If it reports the user chose to skip
license-header generation, respect that choice for the rest of this run — don't ask again on later tasks.

Once the sub-skill (or the direct implementation, for an unresolved key) reports a clean outcome, use its summary
to proceed to Step 5 for this task, then continue to the next unchecked task.

## Step 5 — Check the task's boxes in `implementation_plan.md`

Immediately after a task clears Step 4 (don't batch this until the end — the file must reflect true progress at
all times in case the run is interrupted):

- Check that task's own checkbox (`[ ]` → `[x]`), and the checkbox of every sub-task under it that was part of
  the completed work.
- Add a short note under the task (one or two lines) naming the files touched, the tests added/updated, the
  coverage achieved, and the code-quality result, e.g. `- [x] **Add `Widget`** — tests in WidgetTest (94% line
  coverage, no new Checkstyle/PMD/SpotBugs issues).` If `java-code-one-task`'s summary (its own Step 11) reported
  a new issue left in place as unavoidable, name it and the reason here instead.
- Save the file, then update the mirrored entry in the session task list (Step 3) to completed.

## Step 6 — When to interrupt the user

Keep interruptions rare — by design, this skill should run start-to-finish unattended on a well-formed plan.
Stop and use `AskUserQuestion` (or plain text if no real choice is being offered) only when:

- The per-task `<key>-code-one-task` skill invoked in Step 4 (`java-code-one-task`, `dotnet-code-one-task`, or
  whichever one matched the task's language/framework) reports it stopped on a blocker for the current task — a
  test that keeps failing after a genuine fix attempt where the failure suggests the plan's described approach is
  actually wrong or infeasible against the real code, or an ambiguity that changes correctness or scope and can't
  be safely inferred (conflicting instructions, a choice only the user can make, a missing decision). Mirror the
  bar used by the `plan` skill's Step 3: don't ask about things with an obvious best-practice answer.
- A tool call needs a permission outside this skill's `allowed-tools` list and the underlying action is risky or
  irreversible (e.g. anything beyond the scoped `mvn`/read-only `git` commands this skill is pre-approved for) —
  in this case the harness will already prompt; just make the case for why it's needed if asked.
- `implementation_plan.md` already has every checkbox checked when a run starts — handled in Step 1, not here;
  don't redo finished work.

Do not stop merely because a task is nontrivial — implement it. Do not check a task's box to move past a
blocker; leave it unchecked and surface the blocker instead (and skip Step 8's documentation update and Step 9's
archiving — an incomplete plan stays at the root so the next run can resume it).

## Step 7 — Final verification

Once every checkbox is checked, confirm the whole build — not just the incrementally-tested pieces — is healthy
by running the full local verification from `CLAUDE.md` in a sub-agent, for the same reason as the other
verification steps in Step 4: `Agent({description: "Run full local verification", prompt: "Run `mvn clean
jacoco:prepare-agent install jacoco:report javadoc:jar source:jar -P '!build-extras'`. If it succeeds, report
back only that the build succeeded. If it fails, report back only the failing module/goal, the failure reason,
and the relevant error output/stack trace — not the full build log."})`. Running this in a sub-agent's separate
context keeps the verbose build output out of the main context window. If it fails, fix it (or, if it's a
genuine blocker per Step 6, stop and surface it) — do not archive a plan behind a broken build. Re-invoke the
same sub-agent pattern after fixing, and repeat until it reports success.

Then assess overall code quality against the baseline(s) captured in Step 3, once per distinct language/framework
key found in the plan:

1. For each key a baseline was captured for in Step 3, run its project-wide quality check by delegating to a
   sub-agent, for the same reason as the other quality checks in this skill: `Agent({description: "Compare final
   quality against baseline (<key>)", prompt: "Invoke Skill({skill: \"<key>-code-quality\"}) unscoped,
   project-wide. Compare the reported issues against this baseline: <that key's baseline from Step 3>. Report back
   only the total issue count per tool and the per-file list of issues — not the raw generated reports."})` — e.g.
   `java-code-quality` for the `java` baseline, `dotnet-code-quality` for the `dotnet` baseline. Running each in
   its own sub-agent's separate context keeps the full reports out of the main context window. Compare each
   returned counts/per-file list against that same key's Step 3 baseline.
2. If, across every key checked, the total issue count did not increase and is zero (or was already zero at
   baseline and still is), overall quality is excellent — proceed to Step 8.
3. Otherwise — one or more keys leave the project with more issues than it started with, or a nonzero count
   persists for one or more of them — do not silently proceed. Use `AskUserQuestion` to tell the user what
   remains (counts and a short summary per tool, grouped by language/framework key if more than one is involved)
   and ask whether they want to:
   - add follow-up task(s) to `implementation_plan.md` to fix the remaining issues before this run archives it
     (if chosen, add the task(s) with empty checkboxes and return to Step 4 to implement them — do not archive
     yet), or
   - file the remaining issues as new GitHub issues instead (`gh issue create`, one issue per distinct problem
     or logical group, with enough detail — file, line, tool, message — to act on later), or
   - leave it as-is and proceed to archive without further action.
   Respect whichever the user picks; only proceed to Step 8 once this choice has been acted on (issues filed, or
   explicit acknowledgment to proceed as-is).
4. If no language/framework key could be determined for the plan, or none of the keys involved have a matching
   `<key>-code-quality` skill installed in this repository, skip this quality assessment entirely and proceed
   directly to the security assessment below. If only some keys have a matching skill, assess just those and note
   in the Step 9 summary which key(s) were skipped for lacking one.

Then assess security against the baseline captured in Step 3 (skip this entire assessment if Step 3 recorded
that security scanning is unavailable for this run, or if the `check-security` skill is unavailable in this
repository):

1. Run the project-wide security check by delegating to a sub-agent, for the same reason as the quality check
   above: `Agent({description: "Compare final security scan against baseline", prompt: "Invoke Skill({skill:
   \"check-security\"}) unscoped, project-wide. Report back only the count and the per-file list of flagged
   secrets (new since the last `.secrets.baseline` update, plus any still lacking a triage label) — not the raw
   detect-secrets output or baseline file contents."})`. Running this in a sub-agent's separate context keeps the
   raw scan output out of the main context window. Compare the returned count/per-file list against the Step 3
   baseline.
2. If the count did not increase (zero flagged secrets beyond what Step 3 already recorded), security is
   clean — proceed to Step 8.
3. If the count increased — this run's tasks introduced a new secret, or left one newly unaudited — treat this
   as a hard stop, unlike the quality gate above: there is no "leave as-is and proceed" option here, since that
   risks committing a real credential. Use `AskUserQuestion` to show the user exactly what's new (file, line,
   secret type) and ask them to either:
   - remove or rotate the real secret(s) now, or
   - mark any false positives themselves via `detect-secrets audit .secrets.baseline` (this skill never performs
     that labeling on the user's behalf — see `check-security`'s own Step 6),
   then re-run this security check (repeat from item 1) until it reports no increase. Do not proceed to Step 8 —
   do not check off, archive, or otherwise treat any task as complete — while an unresolved increase remains.
   This is what keeps a leaked secret from ever reaching a state that this skill, or any skill/session that picks
   up after it (e.g. `pr-description`, `issue`, or the user's own next `git commit`), could commit, push, or
   include in a pull request.
4. If the `check-security` skill is unavailable in this repository, or Step 3 recorded that scanning was skipped
   for this run, skip this security assessment and proceed to Step 8 directly.

## Step 8 — Update documentation

Once Step 7 passes clean (healthy build, and the quality and security gates both resolved), bring the project's
Antora documentation in line with everything this run actually changed, by delegating to the `update-docs` skill:
`Skill({skill: "update-docs"})`. Invoked with no argument, `update-docs` covers uncommitted changes plus any
commits already made on the current branch that aren't on the base branch — which is exactly this run's surface,
since Step 9 hasn't committed or archived anything yet. That means it picks up every task processed across all of
Step 4's iterations, not just the last one, so the docs reflect the plan's full scope rather than only its final
task.

- If this repository has no Antora documentation site, `update-docs` should report that plainly rather than
  fail — treat that as nothing to do here and continue to Step 9.
- If `update-docs` needs to ask something only the user can resolve (per its own rules), let it surface via
  `AskUserQuestion` rather than guessing on its behalf.
- Note what `update-docs` changed (which pages, for which behavior) so it can be folded into the Step 9 summary
  alongside the code changes.

Then continue to Step 9.

## Step 9 — Archive the plan and ask for review

Only after Step 7 passes clean, with every task's checkbox checked, both the quality and security gates above
resolved, and Step 8's documentation update done:

1. Determine an identifier for the archived filename:
   - If the plan was grounded in a GitHub issue (check the "Task summary" section and the plan's origin for an
     issue reference, e.g. "issue #42"), use that issue id.
   - Otherwise, use the current timestamp: `date +%Y%m%d%H%M%S`.
2. Create the archive directory if it doesn't exist yet (`mkdir -p .archive`), then move the plan there, renamed
   to embed that identifier: `mv implementation_plan.md .archive/implementation_plan_<issue_id>.md` (e.g.
   `.archive/implementation_plan_42.md` or `.archive/implementation_plan_20260707153000.md`). The repository
   root must no longer have an `implementation_plan.md` once this step completes successfully.
3. Summarize for the user: which tasks were completed, the files touched overall, the coverage achieved per
   task/class, the final code-quality outcome (excellent / follow-up filed / issues left as-is per Step 7), the
   final security outcome (clean / not run, since a resolved-but-triggered gate is otherwise indistinguishable
   from clean once Step 7 passes), what Step 8 updated in the documentation (or that there was no documentation
   site to update), and where the plan was archived to.
4. Explicitly ask the user to review the code and documentation changes (e.g. `git diff` / `git status`) before
   anything is committed. Do not commit, push, or open a PR — this skill's job ends at working, tested, reviewable
   code. This applies with extra force if Step 7's security gate ever fired during this run: even though it must
   have been
   resolved to reach this point, remind the user to double-check the resolution (secret actually removed/rotated,
   or genuinely a false positive) before they or any subsequent skill commits, pushes, or opens a PR.
