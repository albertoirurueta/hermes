---
name: explore
description: Explore this repository codebase, optionally grounded in a specific GitHub issue used as the task to investigate. Invoke as `/explore <issue-id>` to focus the exploration on that issue. If invoked as `/explore` with no argument, ask the user for an issue ID; if they decline or give none, just explore the current codebase. Looks up issues with the `gh` CLI (or GitHub MCP tools if `gh` is unavailable). Use for onboarding, understanding "what would it take to fix issue #N", or getting oriented before planning work.
---

# Explore

Produce a grounded understanding of this repository codebase and, when available, of a specific GitHub issue —
without writing a plan or making code changes. This skill is read-only research: its output is a report, not a
diff.

## Step 1 — Resolve the issue ID

The skill may be invoked with an issue ID as its argument (e.g. `/explore 42`).

- **Argument provided**: use it as the issue ID directly, skip to Step 2.
- **No argument provided**: ask the user (via `AskUserQuestion`) for a GitHub issue ID to focus the exploration
  on, making clear they can skip this and just have the codebase explored instead. Offer something like:
  - "Provide an issue ID" (free text via "Other")
  - "No issue — just explore the codebase" (recommended default if they seem in a hurry)
  - If they decline or give no ID, proceed directly to Step 3 (codebase-only exploration).

## Step 2 — Fetch the issue

Determine the repository (owner/name) from the git remote, e.g.:

```bash
gh repo view --json owner,name -q '.owner.login + "/" + .name'
```

Then fetch the issue with the `gh` CLI:

```bash
gh issue view <issue-id> --json number,title,body,labels,comments,state,url
```

If `gh` is not installed/authenticated, search for GitHub MCP tools instead (`ToolSearch` with a query like
"github issue") and use those to fetch the same information. If neither is available, tell the user and continue
with a codebase-only exploration (Step 3), noting the issue could not be retrieved.

Read the issue title, body, labels, and any comments that clarify scope or add constraints. Note explicitly:
what behavior is requested/reported, any acceptance criteria, and any files, classes, or error messages
mentioned.

## Step 3 — Explore the codebase

This step must work generically on any repository — don't assume a language, framework, or prior familiarity
with the project. Start from first principles each time:

- Get the lay of the land first: check for a `CLAUDE.md`, `README`, or `AGENTS.md` (use as a starting index,
  never as a substitute for reading the actual files, and treat it skeptically if it looks stale), list the
  top-level directories, and identify the language(s), build/package tooling, and entry points (main modules,
  services, public API surface).
- Use the `Explore` agent (or direct `Read`/`grep` for small, targeted lookups) to build a picture of the
  most significant parts of the code structure: how the project is organized into modules/packages, the core
  abstractions and how they relate (inheritance, composition, key interfaces), where the main control flow or
  request/data path runs, and where tests live and how they map to source.

**If an issue was fetched**, narrow the exploration to only the areas the issue implicates — do not do a full
general tour. Concretely:
  - Search for the classes/functions/files/error messages/behaviors named or described in the issue.
  - Read those files plus their closest structural neighbors (the base class/interface they implement, direct
    callers/callees) — only as much surrounding context as needed to understand how that area currently
    behaves.
  - Check existing tests covering that area to learn current expected behavior and edge cases already handled.
  - Skip unrelated parts of the codebase, even if they'd normally be worth a general orientation pass.

**If there is no issue** (declined or unavailable), do a general orientation pass instead: map out the overall
architecture — the major components/modules, how they depend on each other, key abstractions and design
patterns in use, and anything notable in recent git history (`git log --oneline -20`) that suggests active
areas of work.

## Step 4 — Report findings

Summarize for the user in plain text (no file output unless asked):

- If an issue was explored: restate the task in your own words, list the specific files/classes/methods
  relevant to it, explain how the current code behaves in the area the issue touches, and flag anything
  ambiguous or missing from the issue that would block implementation.
- If codebase-only: summarize the architecture areas covered and anything notable found (e.g. inconsistencies,
  undocumented behavior, missing test coverage) relevant to general orientation.
- Do not propose an implementation plan or start editing code — if the user wants that next, they will ask for
  it (e.g. via planning mode) as a separate step.
