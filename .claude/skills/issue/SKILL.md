---
name: issue
description: End-to-end kickoff for a GitHub issue — requires an issue ID, unlike `explore`/`plan` this skill stops if none is given. Fetches the issue, classifies it as a feature or a hotfix from its labels/content, creates a `feature/<issue-id>` or `hotfix/<issue-id>` branch off the current branch, then runs the `explore` and `plan` skills for that issue. Once a reviewable `implementation_plan.md` exists, asks whether to hand off implementation to the `code` skill (run in an isolated sub-agent) or stop for manual review; once implementation is done, pushes the branch, opens a pull request back to the branch `/issue` was run from, uses the `pr-description` skill to fill in its description, then runs the `pr-review` skill (passing both the new PR's id and the issue id) to leave review comments on it. Invoke as `/issue <issue-id>`. Use when starting work on a tracked issue and you want branch creation, exploration, planning, implementation, PR creation, and an initial code review done in one pass.
---

# Issue

Take a GitHub issue from "just filed" to "pull request open for review, with review comments already left" in one
pass. This skill orchestrates `explore`, `plan`, `code`, `pr-description`, and `pr-review` — it does not duplicate
their logic, it sequences them around branch creation, issue triage, and PR creation.

## Step 1 — Resolve the issue ID (required)

Unlike `explore`/`plan`, this skill has no "no issue" mode — an issue ID is mandatory.

- **Argument provided** (e.g. `/issue 42`): use it, go to Step 2.
- **No argument provided**: ask the user via `AskUserQuestion` for the GitHub issue ID to work on (free text).
  - If they provide one, use it and continue to Step 2.
  - If they decline or give no ID, stop here entirely. Tell the user this skill requires an issue ID and that
    they can re-invoke it with one (e.g. `/issue 42`). Do not fall back to a codebase-only exploration or any
    other partial behavior.

## Step 2 — Fetch the issue

Determine the repository (owner/name) from the git remote:

```bash
gh repo view --json owner,name -q '.owner.login + "/" + .name'
```

Then fetch the issue:

```bash
gh issue view <issue-id> --json number,title,body,labels,comments,state,url
```

If `gh` is not installed/authenticated, search for GitHub MCP tools instead (`ToolSearch` with a query like
"github issue") and use those to fetch the same fields. If the issue can't be fetched by any means, tell the user
and stop — every later step (classification, branch naming, `explore`, `plan`) depends on it.

## Step 3 — Classify the issue as feature or hotfix

Decide whether the issue describes a new feature or a hotfix/bug fix, in this order of precedence:

1. **Labels first**: labels like `bug`, `fix`, `hotfix`, `regression` → hotfix; labels like `feature`,
   `enhancement`, `feature-request` → feature. Use whichever category the issue's actual labels most clearly map
   to.
2. **Title/body content** if labels are absent or don't clearly indicate either category: language describing
   broken/incorrect existing behavior (e.g. "fails", "crash", "regression", "broken", "incorrect", "error when…")
   points to hotfix; language describing new, not-yet-existing capability (e.g. "add", "support for", "new",
   "implement", "as a user I want…") points to feature.
3. **If still genuinely ambiguous** after both checks (e.g. conflicting signals, or the issue reads as a bit of
   both): ask the user via `AskUserQuestion` to pick feature or hotfix, showing the issue title and your reasoning
   for why it's ambiguous, with your best guess as the recommended default.

State which category was chosen and why (labels vs. content) before moving on — this determines the branch prefix
in Step 4.

## Step 4 — Create the branch

- Capture the current branch name first (`git branch --show-current`) and remember it as `<base-branch>` — this is
  the branch `/issue` was invoked from (typically `develop`, `main`, or `master`), and is the destination the pull
  request in Step 7 will target.
- Compute the branch name: `feature/<issue-id>` if Step 3 classified it as a feature, `hotfix/<issue-id>` if a
  hotfix.
- Check it doesn't already exist locally or on `origin` (`git branch --list <branch-name>`, `git ls-remote --heads
  origin <branch-name>`). If it does, tell the user and ask (`AskUserQuestion`) whether to check it out instead of
  creating it, pick a different name, or stop — don't silently overwrite or reuse it.
- Otherwise, create it from `<base-branch>`: `git checkout -b <branch-name>`. This is a local, easily reversible
  action (no push), consistent with how this repository's other skills branch without extra confirmation — so no
  need to ask before this specific step, only report which branch was created and from what base afterward.

## Step 5 — Explore and plan

Run, in order, waiting for each to finish before starting the next:

1. `Skill({skill: "explore", args: "<issue-id>"})`
2. `Skill({skill: "plan", args: "<issue-id>"})`

`plan`'s own Step 2 already checks whether exploration for this issue happened earlier in the conversation and
skips re-running `explore` if so — since Step 5.1 just did that exploration, `plan` should detect and reuse it
rather than duplicating the work. That's expected; don't intervene.

If either skill reports it cannot proceed (e.g. `plan` needed a clarification the user declined to resolve), stop
here and surface that to the user rather than pushing ahead to Step 6 with an incomplete plan.

## Step 6 — Choose how to proceed with implementation

Once `implementation_plan.md` exists at the repository root, ask the user via `AskUserQuestion`:

- **Proceed automatically with the `code` skill** — hands off implementation now; a pull request is opened
  automatically once it finishes (Step 7).
- **Review manually first** — the user reads/edits `implementation_plan.md` themselves and runs `/code` later,
  whenever they're ready.

Handle the choice as follows:

- **Review manually**: stop here (skip Step 7). Tell the user the branch that was created, that
  `implementation_plan.md` is ready for review at the repository root, and that once they've run `/code`
  themselves and are happy with the result, they can invoke the `pr-description` skill (or ask you to) to push the
  branch and open a pull request back to `<base-branch>`.
- **Proceed automatically**: implementation must run with a clean context — carrying this whole
  exploration/planning transcript into `code`'s execution would waste context and risk it anchoring on
  intermediate exploration detail instead of the finished plan. Delegate the entire `code` run to a sub-agent
  rather than running it inline, since a sub-agent starts from a fresh context by construction and — unlike
  clearing this conversation directly — lets this skill keep running afterward to open the PR in Step 7:

  ```
  Agent({
    description: "Implement issue <issue-id> via the code skill",
    prompt: "Invoke Skill({skill: \"code\"}) to execute implementation_plan.md at the repository root end to end.
      Report back: which tasks completed, the files touched, the coverage and code-quality outcome, and whether
      the plan was archived. If the skill stops for a genuine blocker instead of finishing, report that blocker
      instead.",
    run_in_background: false
  })
  ```

  Wait for the sub-agent to finish (`run_in_background: false` — Step 7 needs its outcome before opening a PR).
  If it reports a blocker instead of completion (e.g. `code` stopped for a decision only the user can make, or a
  build stayed broken), surface that to the user and stop here — do not proceed to Step 7 against unfinished or
  broken work.

## Step 7 — Push the branch and open the pull request

Only reached when Step 6's sub-agent reports successful completion.

1. **Confirm with the user** before taking any visible/shared-state action: show the branch name, the destination
   (`<base-branch>` from Step 4), and a summary of what the sub-agent changed (from its report). Pushing and
   opening a PR are exactly the kind of actions this repository's other skills (`release`, `pr-description`) always
   confirm before taking — do the same here.
2. Push the branch: `git push -u origin <branch-name>`.
3. Open the pull request with a minimal placeholder body that keeps the issue linked, mirroring how the `release`
   skill opens its PR before handing off to `pr-description`:
   ```bash
   gh pr create --base <base-branch> --head <branch-name> --title "<issue-title> (#<issue-id>)" \
     --body "Closes #<issue-id>."
   ```
4. Invoke `Skill({skill: "pr-description"})`. It will detect the PR just opened (same branch, via its own Step 2),
   draft a description from the actual diff, and ask whether to replace the placeholder body — confirm yes so the
   real description lands.
5. After `pr-description` finishes, check the PR body (`gh pr view <number> --json body`) still references
   `#<issue-id>` (e.g. via a "Closes #<issue-id>" line) — `pr-description` fully replaces the body with its own
   draft and may drop the placeholder's issue link. If the reference is gone, append it back:
   `gh pr edit <number> --body "$(gh pr view <number> --json body -q .body)"$'\n\n'"Closes #<issue-id>."` so the
   issue still auto-closes when the PR merges.
6. Invoke `Skill({skill: "pr-review", args: "<number> <issue-id>"})` to get an initial code review left on the PR
   — passing the issue id explicitly (rather than relying on `pr-review`'s own body/title detection) since this
   skill already knows with certainty which issue the new PR closes. `pr-review` runs its own confirmation before
   posting anything, so no separate confirmation is needed here; wait for it to finish before moving to Step 8.
   If it reports it couldn't proceed (e.g. no PR could be fetched, which shouldn't happen since it was just
   created), surface that to the user rather than treating it as a hard failure of this skill — the PR itself is
   still open and usable.

## Step 8 — Report

Summarize for the user: the issue ID and title, the classification (feature/hotfix) and why, the branch created
and its base, and the final outcome — either manual review pending (Step 6), or the sub-agent's implementation
summary, the PR URL, confirmation the issue is linked, and `pr-review`'s outcome (findings posted, or the drafted
review if the user declined to post) (Step 7).
