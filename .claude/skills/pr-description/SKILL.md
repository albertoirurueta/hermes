---
name: pr-description
description: Check the current branch's code changes and draft a brief, concise pull request description (emojis and diagrams allowed where they genuinely clarify). Detects which platform hosts the repository — GitHub, Bitbucket, Azure DevOps, or TFS — and uses that platform's own tooling throughout instead of assuming `gh`/GitHub. If a pull request already exists for the current branch, offers to update its body with the draft; otherwise asks whether to commit the changes to a new branch and open a new pull request, prompting for branch name, destination branch, and PR title. Invoke as `/pr-description`. Use whenever the user wants a PR description generated from the actual diff instead of writing it by hand.
---

# PR Description

Draft a pull request description from the real code changes, then either apply it to an existing PR or help open
a new one. This skill never pushes, commits, or opens/edits a PR without the user explicitly confirming the
specific action first.

## Step 1 — Determine the repository host and available tooling

Before touching any PR, determine which platform hosts this repository, so the right tool is used throughout
instead of assuming GitHub/`gh`:

- If `explore` already ran earlier in this conversation (e.g. this skill was invoked as part of `issue`, after its
  Step 5 exploration), reuse the `Repository host: ...` line from its Tech stack report rather than re-detecting.
- Otherwise, detect it directly — the same way `explore`'s own Step 2 does: get the remote URL (`git remote
  get-url origin`, falling back to `git remote -v` or whichever remote the current branch tracks) and match it
  against known patterns:
  - `github.com` (or a GitHub Enterprise Server domain) → **GitHub**. Tooling: the `gh` CLI, or GitHub MCP tools
    (`ToolSearch` "github").
  - `bitbucket.org` (cloud), or a self-hosted domain with a `/scm/<project>/<repo>.git` path (Bitbucket
    Server/Data Center) → **Bitbucket**. Tooling: search for Bitbucket MCP tools first (`ToolSearch` "bitbucket"),
    otherwise the Bitbucket REST API.
  - `dev.azure.com` or `<org>.visualstudio.com` → **Azure DevOps (cloud)**. Tooling: `az repos` (the Azure CLI
    with the `azure-devops` extension) if installed, or Azure DevOps MCP tools (`ToolSearch` "azure devops").
  - A self-hosted domain with a `/tfs/` path segment, or bare `_git` in the path without a `dev.azure.com`/
    `visualstudio.com` domain → **Azure DevOps Server / TFS**. Same tooling family as Azure DevOps, but confirm
    the collection URL and API version before assuming parity.
  - If the URL doesn't clearly match any of these, ask the user (`AskUserQuestion`) which platform it is rather
    than guessing.
- If there's no remote configured at all, tell the user a PR can't be inspected or created without a hosted
  remote, and stop — every step from here on assumes one exists (or will, once the branch is pushed).

Record the detected host — every later step's "use the host's tool" instruction refers back to this.

## Step 2 — Determine the diff to describe

- Determine the current branch: `git branch --show-current`.
- Determine the repository's base branch: `git symbolic-ref refs/remotes/origin/HEAD` (strip the
  `refs/remotes/origin/` prefix), falling back to `main`/`master` if that ref isn't set, or asking the user if
  genuinely ambiguous.
- Gather the full picture of changes relative to that base:
  - Commits already on this branch but not on base: `git diff <base>...HEAD`
  - Anything not yet committed: `git status`, `git diff`, and `git diff --staged`
- If there is nothing to describe (no commits ahead of base, no staged/unstaged changes, no untracked files), tell
  the user and stop rather than inventing a description.
- Read the full diff, not just file names or stat summaries — the description must be grounded in what actually
  changed.

## Step 3 — Check whether a pull request already exists

Use the tool matching the host detected in Step 1:

- **GitHub**:
  ```bash
  gh pr view --json number,title,url,baseRefName,headRefName,body 2>/dev/null
  ```
  If that returns nothing (e.g. the branch hasn't been pushed yet), also try:
  ```bash
  gh pr list --head <branch> --json number,url,title,baseRefName,body
  ```
  If `gh` is unavailable/unauthenticated, search for GitHub MCP tools instead (`ToolSearch` "github pull
  request").
- **Bitbucket**: look up open pull requests via Bitbucket MCP tools if connected, otherwise the REST API (`GET
  /2.0/repositories/<workspace>/<repo>/pullrequests?q=source.branch.name="<branch>"` for Cloud, or the Server/Data
  Center equivalent) — find the one whose source branch matches the current branch.
- **Azure DevOps / TFS**: `az repos pr list --source-branch <branch> --status active`, or Azure DevOps MCP tools
  if connected — find the one matching the current branch.

If no tool for the detected host is available at all (no CLI installed/authenticated and no matching MCP
connection), tell the user and treat this as "no PR exists" — skip straight to Step 4, and in Step 6 only offer
the printed description rather than attempting to create one.

## Step 4 — Draft the description

Compose a description from the diff (and commit messages, for context on intent):

- Keep it brief — a short summary of what changed and why, not a line-by-line narration of the diff.
- Structure loosely as one or two summary sentences, then a short bulleted list of the concrete changes if there
  is more than one logical change. Omit any section that would be empty or redundant.
- Emojis are fine, used sparingly — e.g. one per bullet to categorize (✨ feature, 🐛 fix, ♻️ refactor, 📝 docs,
  ✅ tests) — don't overuse them.
- A small Mermaid diagram (` ```mermaid ` fenced block) is fine when a change reshapes control/data flow in a way
  a diagram clarifies faster than prose — GitHub and Azure DevOps render Mermaid natively in PR/description
  bodies; Bitbucket does not, so skip it there (or check first if unsure). Skip it for straightforward changes
  regardless — most PRs don't need one.
- Don't invent behavior that isn't in the diff, and don't editorialize about code quality.

Show the drafted description to the user before taking any further action.

## Step 5 — Existing PR: offer to update it

If Step 3 found an existing PR:

- Show its number, URL, and current body next to the new draft.
- Ask the user whether to replace the PR's body with the draft.
- If yes: apply it with the tool matching the host detected in Step 1:
  - **GitHub**: write the draft to a temp file and run `gh pr edit <number> --body-file <path>` (a temp file
    avoids shell-quoting issues with multi-line/Markdown content).
  - **Bitbucket**: update the description via Bitbucket MCP tools if connected, otherwise a REST API call to the
    pull request's update endpoint with the new description field.
  - **Azure DevOps / TFS**: `az repos pr update --id <number> --description "$(cat <path>)"`, or Azure DevOps MCP
    tools if connected.
  This edits shared, visible state — proceed only after the user's explicit yes.
- If no: stop — the draft has already been shown, there's nothing else to do.

## Step 6 — No PR: offer to commit and open one

If Step 3 found no existing PR, ask the user (via `AskUserQuestion`) whether they want to commit the current
changes to a new branch and open a pull request now, or just keep the drafted description for manual use.
Creating branches, committing, pushing, and opening a PR are all visible and/or hard-to-reverse actions — never
do any of them without this explicit confirmation.

If they decline: stop — the draft from Step 4 is the deliverable.

If they accept, collect the specifics via `AskUserQuestion`, offering an inferred default for each so the user
can accept or override in one step:

- **Branch name** — default: a kebab-case slug derived from the draft's summary (e.g.
  `fix-list-item-move-detection`). If the current branch is already a non-default branch with commits ahead of
  base and no uncommitted changes, ask instead whether to reuse it rather than creating a new one.
- **Destination branch** — default: the base branch found in Step 2.
- **Pull request title** — default: a concise, imperative one-liner derived from the draft.

Then, in order:

1. If there are uncommitted changes, create the branch (`git checkout -b <branch-name>`), stage the relevant
   files by name (avoid `git add -A`/`git add .`, and check contents of anything unfamiliar before staging in
   case it holds secrets), and commit with a message consistent with this repo's recent style
   (`git log --oneline -10` for reference). If reusing the current branch per the note above, skip straight to
   step 2.
2. Push the branch: `git push -u origin <branch-name>`.
3. Write the Step 4 draft to a temp file and open the PR with the tool matching the host detected in Step 1:
   - **GitHub**: `gh pr create --base <destination-branch> --head <branch-name> --title "<title>" --body-file
     <path>`.
   - **Bitbucket**: Bitbucket MCP tools if connected, otherwise `POST
     /2.0/repositories/<workspace>/<repo>/pullrequests` (Cloud) or the Server/Data Center equivalent, with the
     title, the temp file's content as the description, the source branch, and the destination branch.
   - **Azure DevOps / TFS**: `az repos pr create --target-branch <destination-branch> --source-branch
     <branch-name> --title "<title>" --description "$(cat <path>)"`, or Azure DevOps MCP tools if connected.
4. Report the PR URL: the create command's own output for GitHub/Azure CLI; the API/MCP response's web link for
   Bitbucket; or `az repos pr show --id <id> --query "_links.web.href" -o tsv` for Azure DevOps/TFS if the create
   step didn't already surface it.

## Step 7 — Report

Summarize what happened: the drafted description, and the outcome — PR body updated (with URL), new PR created
(with URL), or description-only with no further action taken.
