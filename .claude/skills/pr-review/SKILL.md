---
name: pr-review
description: Review a GitHub pull request's changes by id. Optionally takes the linked GitHub issue id as a second argument (skipping auto-detection); otherwise looks for a link in the PR body/title (e.g. "Closes #N") and fetches that issue for context on intent. Runs the `explore` skill (grounded in the linked issue when there is one) to understand the codebase's current architecture and conventions before judging the diff. When the `code-review` skill is available, delegates the correctness/reuse/simplification/efficiency analysis to it (via its `--comment` flag) rather than duplicating that logic, and layers on its own checks for issue-intent alignment and codebase-convention drift; if `code-review` isn't available, runs the full review itself. Drafts its own findings and — after explicit confirmation — posts them as inline review comments on the pull request for manual follow-up. Invoke as `/pr-review <pr-id> [issue-id]`. Use when the user wants an actual PR reviewed and commented on, not a description generated (`pr-description`).
---

# PR Review

Review a specific, already-open GitHub pull request and leave the findings as review comments on it — this skill
never edits code itself. It is grounded in this repository's actual architecture and conventions, learned fresh
via the `explore` skill, plus the originating issue's intent when the PR links one. Where the generic `code-review`
skill is available, this skill prefers delegating to it for correctness/reuse/simplification/efficiency findings
(it already knows how to post inline PR comments) rather than reimplementing that analysis, and focuses its own
review on what `code-review` has no way to know: whether the change matches the linked issue's intent and whether
it follows this codebase's own conventions.

## Step 1 — Resolve the PR id (required) and issue id (optional)

An issue ID is optional but a PR id is not — there is nothing to review without it.

- **PR id argument provided** (e.g. `/pr-review 17`, or `/pr-review 17 42` to also pass the issue id explicitly):
  use it, go to Step 2. If a second argument is present, treat it as the linked issue id and use it directly in
  Step 3 — skip that step's own detection since the caller (e.g. the `issue` skill, which knows exactly which
  issue a PR it just opened closes) already knows the relationship with certainty.
- **No PR id provided**: ask the user via `AskUserQuestion` for the pull request number. If they decline or give
  none, stop here entirely and tell them this skill requires a PR id (e.g. `/pr-review 17`).

## Step 2 — Fetch the pull request

Determine the repository (owner/name) from the git remote:

```bash
gh repo view --json owner,name -q '.owner.login + "/" + .name'
```

Fetch the PR's metadata, diff, and any comments/reviews already left on it:

```bash
gh pr view <pr-id> --json number,title,body,url,state,baseRefName,headRefName,headRefOid,additions,deletions,files,commits
gh pr diff <pr-id>
gh api repos/<owner>/<repo>/pulls/<pr-id>/comments
```

If `gh` is not installed/authenticated, search for GitHub MCP tools instead (`ToolSearch` with a query like "github
pull request") and use those to fetch the same information. If the PR can't be fetched by any means, tell the user
and stop.

If the PR is already merged or closed, tell the user and ask (`AskUserQuestion`) whether to continue anyway (e.g.
a post-hoc review) or stop — don't assume either way.

Read the full diff, not just the file list or a stat summary — the review must be grounded in the actual changes.
Note the existing comments/reviews so Step 6 doesn't re-raise points already made.

## Step 3 — Find a linked GitHub issue, if any

If Step 1 already received an explicit issue id, skip detection and use it directly.

Otherwise, look for an issue reference in the PR title or body: patterns like `Closes #N`, `Fixes #N`,
`Resolves #N`, or a bare `#N` mention.

Either way, once an issue id is known, fetch it the same way the `explore`/`issue` skills do:

```bash
gh issue view <issue-id> --json number,title,body,labels,comments,state,url
```

If no reference is found/provided, or the issue can't be fetched, continue without one — this skill works fine on
a PR with no linked issue, it just loses that piece of intent context.

## Step 4 — Explore the codebase

The review must be grounded in how this codebase is actually structured, not just the diff in isolation.

- **Check first**: if this conversation already explored the codebase (or this same issue) recently, reuse that
  instead of repeating it.
- **Otherwise**, invoke the `explore` skill: `Skill({skill: "explore", args: "<issue-id>"})` if Step 3 found an
  issue, or `Skill({skill: "explore"})` with no argument otherwise. Wait for it to finish before continuing.

From the exploration, note in particular: the architectural patterns and abstractions the changed files should be
consistent with, any contributor conventions documented in `CLAUDE.md`/`README`/`AGENTS.md` (naming, error
handling, validation, documentation requirements, etc.), and the language/tooling in play (so Step 6 can apply
the right idioms and check for the right lint/static-analysis config — e.g. Checkstyle/PMD/SpotBugs rules for
Java, an ESLint/Prettier config for JS/TS, and so on — read any such config found rather than assuming defaults).

## Step 5 — Delegate to the `code-review` skill when available

Check the currently available skills (listed in this session) for `code-review`. It already knows how to find
correctness bugs and reuse/simplification/efficiency cleanups and how to post them as inline PR comments — don't
reimplement that analysis if it's available.

- **If `code-review` is not available**: skip this step entirely and cover correctness, security, test coverage,
  and simplification/reuse/efficiency yourself in Step 6 alongside the issue-intent and convention checks.
- **If `code-review` is available**: it reviews "the current diff", so it needs the PR's changes checked out
  locally first:
  1. Capture the current branch (`git branch --show-current`) so it can be restored afterward, and run `git
     status` first per this repository's safety conventions — if there are uncommitted changes, stash them
     (`git stash push -u`) rather than checking out over them.
  2. Check out the PR: `gh pr checkout <pr-id>`.
  3. Tell the user this skill is about to invoke `code-review` with `--comment`, which posts its findings to the
     PR directly and immediately (it does not stop for a separate confirmation) — get explicit confirmation
     before proceeding, since Step 7's confirmation gate only covers this skill's own findings, not
     `code-review`'s.
  4. Invoke it: `Skill({skill: "code-review", args: "high --comment"})` (use `high` effort for a thorough pass;
     drop to `medium` if the user wants a lighter review). Wait for it to finish.
  5. Restore the original branch (`git checkout <original-branch>`, and `git stash pop` if a stash was created in
     step 1).
- Note what `code-review` found (or that it wasn't available) for Step 8's report.

## Step 6 — Review for issue intent and convention drift

This step always runs, regardless of whether Step 5 delegated — `code-review` has no visibility into the linked
issue or this repository's specific conventions, so this is this skill's own irreplaceable contribution. If Step
5 was skipped (no `code-review` available), also cover correctness, security, and test coverage here, per the
full list below.

For every changed hunk in the Step 2 diff, evaluate it against the linked issue's intent (if any, from Step 3) and
the codebase's own architecture and conventions (from Step 4). Look for:

1. **Issue-intent mismatch** — the change doesn't actually satisfy what the linked issue asked for, addresses only
   part of it, or introduces behavior the issue didn't request.
2. **Convention/architecture drift** — code that doesn't follow the patterns Step 4 identified (e.g. bypassing an
   existing abstraction, inconsistent validation/error-handling style, missing required documentation like
   Javadoc where this repo's conventions mandate it).

If Step 5 didn't run, also look for, using general best practices for the language in use:

3. **Correctness bugs** — logic errors, edge cases the code doesn't handle, incorrect assumptions, concurrency
   issues, resource leaks.
4. **Security issues** — anything resembling the OWASP top 10 (injection, unsafe deserialization, missing input
   validation at boundaries, secrets in code, etc.) relevant to the language/framework in play.
5. **Test coverage** — new/changed behavior without a corresponding test, or tests that don't actually exercise
   the edge cases the change introduces.
6. **Simplification/reuse/efficiency** — real, non-cosmetic opportunities: duplicated logic that already exists
   elsewhere in the codebase, unnecessary complexity, obvious inefficiencies. Skip nitpicks and pure style
   preferences that aren't backed by a documented convention.

For each finding, record: the file path, the line number *as it appears in the new version of the file in this
diff* (needed for inline comments in Step 8 — pick a line that is actually part of a diff hunk), a one-sentence
summary of the problem, and a concrete suggested fix. Skip anything that duplicates a point already made in the
existing comments/reviews gathered in Step 2, or that `code-review` already posted in Step 5.

If the diff is large, it's fine to work through it file by file rather than holding the whole thing in view at
once — but every changed file must be considered, not just a sample.

## Step 7 — Draft the review and confirm before posting

Posting comments on a pull request is a visible, shared-state action — never do it without explicit confirmation.

1. Present the drafted findings to the user: grouped by file, each with its line, summary, and suggested fix, plus
   a short overall summary sentence or two for the review body.
2. Ask the user (`AskUserQuestion`) whether to post these as review comments on the PR, and if so, confirm the
   scope (e.g. they may want to drop some findings first).
3. If they decline: stop here — the drafted findings are the deliverable, available for them to act on manually.

## Step 8 — Post the review

Only reached after explicit confirmation. Bundle every confirmed finding into a single pull request review
(one review event, multiple inline comments) rather than posting each as a separate top-level comment — this
mirrors how a human reviewer leaves a batch of inline notes plus a summary, and avoids spamming notifications.

Write the payload to a file in the scratchpad directory, e.g.:

```json
{
  "commit_id": "<headRefOid from Step 2>",
  "event": "COMMENT",
  "body": "<one- or two-sentence overall summary>",
  "comments": [
    { "path": "<file>", "line": <line>, "side": "RIGHT", "body": "<summary + suggested fix>" }
  ]
}
```

Then submit it:

```bash
gh api repos/<owner>/<repo>/pulls/<pr-id>/reviews --method POST --input <path-to-json-file>
```

Always use `event: "COMMENT"` — never `REQUEST_CHANGES` or `APPROVE` — since these are suggestions for the user to
judge, not a verdict.

If the API call fails (e.g. a line isn't part of the diff hunk GitHub allows commenting on), retry that specific
comment on the nearest valid line within the same hunk, or fall back to including it in the overall review `body`
text with an explicit `file:line` reference rather than dropping it silently.

## Step 9 — Report

Summarize for the user: the PR reviewed and its linked issue (if any); whether `code-review` was available and
delegated to (and what it found), or that this skill covered the full scope itself; how many of its own findings
were raised and in which files; and the outcome — a link to the posted review (`gh pr view <pr-id> --json url -q
.url`), or, if they declined to post, a reminder that the draft from Step 7 is the deliverable.
