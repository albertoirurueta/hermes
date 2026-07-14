---
name: create-github-issue
description: Draft and file a new GitHub issue that is ready to be picked up by the `/issue`, `/plan`, `/code` flow. Runs the `explore` skill first to ground the issue in the actual codebase, then asks the user for the purpose of the task to be implemented, then asks for additional context (linked URLs, attached files/documents, related issues) and folds whatever is provided into the issue body. Classifies the issue as bug/feature/enhancement from the stated purpose and matches it against this repository's actual label set, drafts a title and body (Summary, Context, Acceptance criteria, References), and shows the draft for confirmation before filing it with `gh issue create` (or GitHub MCP tools if `gh` is unavailable). Invoke as `/create-github-issue` or `/create-github-issue <short description>`. Stops early if this repository isn't hosted on GitHub. Use when the user has an idea, bug report, or request that needs to become a well-formed, actionable GitHub issue before `/issue` or `/plan` can pick it up — not for issues that already exist (nothing to file) or repositories on Bitbucket/Azure DevOps/TFS/Jira (no issue-filing target this skill supports).
---

# Create GitHub Issue

Turn a rough idea, bug report, or request into a well-formed GitHub issue — grounded in the real codebase, with a
clear purpose statement and whatever supporting context the user has — so it's immediately actionable by the
`/issue` or `/plan` skills. This skill only files an issue; it does not branch, plan, or write code.

## Step 1 — Confirm this repository is hosted on GitHub

This skill is GitHub-specific (unlike `explore`/`issue`, which support multiple hosts) — verify before doing any
other work:

- If `explore` already ran earlier in this conversation, reuse its `Repository host: ...` finding instead of
  re-detecting.
- Otherwise, get the remote URL (`git remote get-url origin`, falling back to `git remote -v` or whichever remote
  the current branch tracks) and check it matches `github.com` or a GitHub Enterprise Server domain.
- If the host is something else (Bitbucket, Azure DevOps/TFS) or there's no remote configured at all, tell the
  user plainly that this skill only files GitHub issues, and stop — don't attempt a different platform's
  equivalent or guess at one.

## Step 2 — Explore the codebase

Run `Skill({skill: "explore"})` with no argument — there is no ticket yet, so this is a general orientation pass,
not one narrowed to a specific area. Skip this if general codebase exploration already happened earlier in this
same conversation and looks reasonably current; reuse it instead of repeating the work.

This grounds later steps in the real architecture, conventions, and tech stack (per `explore`'s own "Tech stack"
report) so the issue can reference concrete files/modules instead of vague descriptions, and so its acceptance
criteria fit how the codebase actually behaves today.

## Step 3 — Ask for the purpose of the task

Ask the user directly, in plain conversational text (not `AskUserQuestion` — the answer is inherently open-ended,
not a choice among fixed options): what problem this issue should solve or what capability it should add, why it
matters, and any acceptance criteria they already have in mind. If the skill was invoked with a short description
argument, treat that as a starting point and ask only for what it leaves unclear (e.g. the "why," or what "done"
looks like) rather than re-asking everything from scratch.

Wait for the user's reply before continuing — the rest of the issue is drafted around this purpose.

## Step 4 — Ask for additional context

Ask the user (again as a plain, open-ended question) whether they have any of the following, making clear all of
it is optional:

- **Linked URL(s)** — design docs, discussions, external references, prior art.
- **Attached file(s) or documents** — screenshots, logs, specs, mockups (local paths).
- **Related issue(s)** — in this repository or elsewhere, that this depends on, relates to, or duplicates.

Gather whatever is provided:

- **URLs**: fetch each with `WebFetch` and note the key points worth folding into the issue — don't just link them
  blind if their content materially shapes scope or acceptance criteria.
- **Files/documents**: `Read` each local path (this tool also handles images and PDFs) and summarize what it
  shows that's relevant to the issue.
- **Related issues**: confirm each with `gh issue view <id> --json number,title,url,state` (or GitHub MCP tools if
  `gh` is unavailable) so the reference in the drafted body is accurate — don't take the user's number on faith
  without checking it resolves to what they mean.

If the user has none of these, proceed with just the purpose from Step 3 — this step is not a blocker.

## Step 5 — Classify the issue and pick a label

Decide whether this is a bug or a feature/enhancement, using the same signal words `issue`'s own classification
step uses: language describing broken/incorrect existing behavior ("fails," "crash," "broken," "incorrect," "error
when…") → bug; language describing new, not-yet-existing capability ("add," "support for," "new," "implement,"
"as a user I want…") → feature/enhancement.

Check which labels this repository actually has: `gh label list --json name,description`. Match the
classification against whatever labels exist (e.g. `bug`, `enhancement`, `feature`) — don't invent a label name
that isn't in that list. If nothing matches closely, or the classification is genuinely ambiguous, ask the user
via `AskUserQuestion` to pick from the repository's actual label list (this is now a concrete decision among known
options, unlike Steps 3-4's open-ended questions).

## Step 6 — Draft the issue

Compose:

- **Title**: a concise, imperative one-liner describing the change (e.g. "Add retry support to the HTTP client,"
  "Fix list item move detection on drag cancel").
- **Body**, structured as:
  - **Summary** — the purpose and motivation from Step 3, in the user's own terms.
  - **Context** — grounded in Step 2's exploration: the relevant existing files/modules/classes and how they
    currently behave, so whoever picks this up (including a future `/explore`/`/plan` run) starts oriented
    instead of cold.
  - **Acceptance criteria** — a short bulleted list, if the user gave any in Step 3 or they can be reasonably
    inferred from the purpose; omit this section entirely rather than inventing criteria that weren't discussed.
  - **References** — any URLs, summarized file/document contents, and confirmed related-issue links from Step 4;
    omit if none were provided.

Show the full drafted title and body to the user before taking any action.

## Step 7 — Confirm and create

Filing an issue is visible to everyone with access to the repository — never do it without explicit confirmation.
Ask the user via `AskUserQuestion`:

- **Create the issue now** (recommended once the draft looks right).
- **Edit the draft first** — take their edits, update the draft, and re-ask.
- **Don't create it** — stop here; the draft itself is the deliverable.

If they confirm creation:

- Write the body to a temp file (avoids shell-quoting issues with multi-line Markdown) and run:
  ```bash
  gh issue create --title "<title>" --body-file <path> --label "<label>"
  ```
  Omit `--label` if Step 5 found no matching label. If `gh` is not installed/authenticated, search for GitHub MCP
  tools instead (`ToolSearch` "github issue") and use those to create the issue with the same title, body, and
  label.
- Capture the created issue's number and URL from the command's own output (or `gh issue view --json number,url`
  immediately after, if the create output didn't surface it).

## Step 8 — Report

Give the user the issue number and URL, and tell them the issue is now ready to be picked up: `/issue <number>`
to kick off branch creation, exploration, planning, implementation, and PR creation in one pass, or `/plan
<number>` alone if they'd rather just get an implementation plan first.
