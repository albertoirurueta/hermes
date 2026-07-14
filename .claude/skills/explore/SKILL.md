---
name: explore
description: Explore this repository codebase, optionally grounded in a specific GitHub issue or Jira ticket used as the task to investigate, and detect the programming language(s) and framework(s) in play — both in the existing codebase and in the requested change — plus whether Antora documentation exists and where, and which platform hosts the repository (GitHub, Bitbucket, Azure DevOps, or TFS). Invoke as `/explore <ticket-id>` where `<ticket-id>` is either a GitHub issue ID (e.g. `42`) or a Jira key (e.g. `PROJ-123`) — the skill auto-detects which. If invoked as `/explore` with no argument, ask the user for a ticket ID; if they decline or give none, just explore the current codebase. Looks up GitHub issues with the `gh` CLI (or GitHub MCP tools if `gh` is unavailable) and Jira tickets via any connected Jira MCP tools. Use for onboarding, understanding "what would it take to fix issue #N"/"ticket PROJ-123", or getting oriented before planning work. Its "Tech stack" findings (languages/frameworks, Antora docs location, repository host) are meant to be reused by later skills in the same conversation (e.g. `plan`, `code`, `update-docs`, code review, branch/PR creation) to pick language/framework-appropriate flows, best practices, and the right git-platform tooling.
---

# Explore

Produce a grounded understanding of this repository codebase and, when available, of a specific GitHub issue or
Jira ticket — without writing a plan or making code changes. This skill is read-only research: its output is a
report, not a diff.

## Step 1 — Resolve the ticket ID and its type

The skill may be invoked with a ticket ID as its argument (e.g. `/explore 42` or `/explore PROJ-123`).

- **Argument provided**: detect which system it belongs to:
  - Matches a Jira key pattern — one or more uppercase letters/digits starting with a letter, a hyphen, then
    digits (e.g. `PROJ-123`, `ABC2-45`) → **Jira**.
  - A bare number, optionally prefixed with `#` (e.g. `42`, `#42`) → **GitHub issue**.
  - If it matches neither pattern clearly, ask the user (via `AskUserQuestion`) whether it's a GitHub issue ID or
    a Jira key.
- **No argument provided**: ask the user (via `AskUserQuestion`) for a ticket ID to focus the exploration on,
  making clear they can skip this and just have the codebase explored instead. Offer something like:
  - "Provide a GitHub issue ID or Jira key" (free text via "Other")
  - "No ticket — just explore the codebase" (recommended default if they seem in a hurry)
  - If they decline or give none, proceed directly to Step 4 (codebase-only exploration).

## Step 2 — Detect the repository host and available tooling

Determine which platform hosts this repository, so later skills that operate on it (branch creation, pull/merge
requests, commit history, CI status, etc.) know which CLI/API to use instead of assuming GitHub by default.

- Get the remote URL: `git remote get-url origin` (fall back to `git remote -v`, or to whichever remote the
  current branch tracks, if `origin` isn't the remote in use).
- Match the URL against known host patterns:
  - `github.com` (or a GitHub Enterprise Server domain — look for a `/api/v3/`-style setup, or ask if uncertain)
    → **GitHub**. Tooling: `gh` CLI, or GitHub MCP tools (`ToolSearch` "github").
  - `bitbucket.org` (cloud), or a self-hosted domain with a `/scm/<project>/<repo>.git` path (the Bitbucket
    Server/Data Center convention) → **Bitbucket**. Tooling: there's no ubiquitous CLI equivalent to `gh` here —
    search for Bitbucket MCP tools first (`ToolSearch` "bitbucket"), otherwise fall back to the Bitbucket REST
    API.
  - `dev.azure.com` or `<org>.visualstudio.com` → **Azure DevOps (cloud)**. Tooling: `az repos`/`az boards` (the
    Azure CLI with the `azure-devops` extension) if installed, or Azure DevOps MCP tools (`ToolSearch`
    "azure devops").
  - A self-hosted domain with a `/tfs/` path segment, or `_git` in the path without a `dev.azure.com`/
    `visualstudio.com` domain → **Azure DevOps Server / Team Foundation Server (TFS)**. Same tooling family as
    Azure DevOps (`az repos` can often target a collection URL directly), but confirm the collection URL and API
    version before assuming parity — older on-prem TFS versions expose a narrower REST API surface than cloud
    Azure DevOps.
  - If the URL doesn't clearly match any of these (e.g. a mirrored or internal git host), ask the user (via
    `AskUserQuestion`) which platform it is rather than guessing — a wrong assumption here would misdirect every
    later git-platform operation.
- If there is no remote configured at all (a purely local repository), note that explicitly — later skills that
  need a hosted platform (PR/merge-request creation, etc.) won't have one to target.
- Record the detected host — and, for Azure DevOps/TFS or self-hosted Bitbucket, the base URL/collection — for
  the report in Step 6. This detection is what later skills should key off instead of re-deriving it or
  defaulting to GitHub-specific tooling (`gh`, "Closes #N", etc.) unconditionally.

This is independent of Step 1: it detects where the **code repository** is hosted, not where the **ticket**
lives — a Jira ticket can reference work in a GitHub, Bitbucket, or Azure DevOps repo, and vice versa.

## Step 3 — Fetch the ticket

### GitHub issue

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
with a codebase-only exploration (Step 4), noting the issue could not be retrieved.

### Jira ticket

Search for connected Jira MCP tools (`ToolSearch` with a query like "jira issue" or "jira ticket"). If one or
more are found, use them to fetch the ticket by key: summary/title, description, issue type, status, labels/
components, comments, and URL.

If no Jira MCP tool is available, tell the user plainly that this environment has no Jira connection, then ask
(via `AskUserQuestion`) whether to:
- paste the relevant ticket content (title/description/comments) directly so exploration can proceed grounded
  in it, or
- continue with a codebase-only exploration (Step 4), noting the ticket could not be retrieved.

Don't guess at Jira REST endpoints or attempt to `WebFetch` a Jira URL directly — Jira instances are normally
authenticated, and an unauthenticated fetch will just fail or return a login page.

### Either way

Read the ticket's title, body/description, labels, and any comments that clarify scope or add constraints. Note
explicitly: what behavior is requested/reported, any acceptance criteria, and any files, classes, error messages,
languages, frameworks, or platforms mentioned.

## Step 4 — Explore the codebase

This step must work generically on any repository — don't assume a language, framework, or prior familiarity
with the project. Start from first principles each time:

- Get the lay of the land first: check for a `CLAUDE.md`, `README`, or `AGENTS.md` (use as a starting index,
  never as a substitute for reading the actual files, and treat it skeptically if it looks stale), list the
  top-level directories, and identify the language(s), build/package tooling, and entry points (main modules,
  services, public API surface).
- Check whether Antora documentation exists and where: look for an `antora.yml` (commonly under `docs/`, but
  confirm the actual path rather than assuming) and, alongside it, an `antora-playbook.yml` and a
  `modules/ROOT/pages` (or other module) tree. Record the docs root path if found — later skills (e.g.
  `update-docs`) need this location, and its content is itself a language/framework signal (see Step 5).
- Use the `Explore` agent (or direct `Read`/`grep` for small, targeted lookups) to build a picture of the
  most significant parts of the code structure: how the project is organized into modules/packages, the core
  abstractions and how they relate (inheritance, composition, key interfaces), where the main control flow or
  request/data path runs, and where tests live and how they map to source.

**If a ticket was fetched**, narrow the exploration to only the areas it implicates — do not do a full general
tour. Concretely:
  - Search for the classes/functions/files/error messages/behaviors named or described in the ticket.
  - Read those files plus their closest structural neighbors (the base class/interface they implement, direct
    callers/callees) — only as much surrounding context as needed to understand how that area currently
    behaves.
  - Check existing tests covering that area to learn current expected behavior and edge cases already handled.
  - Skip unrelated parts of the codebase, even if they'd normally be worth a general orientation pass.

**If there is no ticket** (declined or unavailable), do a general orientation pass instead: map out the overall
architecture — the major components/modules, how they depend on each other, key abstractions and design
patterns in use, and anything notable in recent git history (`git log --oneline -20`) that suggests active
areas of work.

## Step 5 — Detect the technology stack

Identify the programming language(s) and framework(s) in play, both for the existing codebase and for whatever
change is being requested. This is not a vague "looks like Java" impression — ground it in concrete evidence, and
call out enough detail (per-module, if the repo has more than one stack) that later skills can act on it directly.

### Codebase languages and frameworks

Use whichever of these signals are present — manifests/config are the most reliable, source-level signals are the
fallback when manifests are absent or ambiguous:

- **Build/package manifests** are the primary signal:
  - `pom.xml`, `build.gradle`/`build.gradle.kts` → Java/Kotlin; check dependencies for `spring-boot-starter*`
    (Spring/Spring Boot), `micronaut-*`, `quarkus-*`, or an Android Gradle plugin (`com.android.application`/
    `com.android.library`) — and within Android, `androidx.compose` deps → Jetpack Compose vs. XML/View-based UI.
  - `*.csproj`/`*.fsproj`/`*.sln` → C#/.NET (or F#); check for `Microsoft.AspNetCore.*` (ASP.NET Core), `*.Maui`/
    Xamarin (cross-platform mobile), WPF/WinForms project types.
  - `package.json` → JavaScript/TypeScript; check `dependencies`/`devDependencies` for `react`, `vue`, `angular`,
    `next`, `express`, `nestjs`, etc.
  - `requirements.txt`, `pyproject.toml`, `Pipfile`, `setup.py` → Python; check for `django`, `flask`, `fastapi`.
  - `Podfile`, `Package.swift`, `*.xcodeproj`/`*.xcworkspace` → Swift and/or Objective-C; check `import SwiftUI`
    vs. `import UIKit` in source files for the UI framework in use, and `.m`/`.mm`/`.h` file presence for
    Objective-C alongside `.swift`.
  - `CMakeLists.txt`, `Makefile`, `*.vcxproj` with `.c`/`.cpp`/`.h`/`.hpp` sources → C/C++.
  - `go.mod` → Go. `Cargo.toml` → Rust. `Gemfile` → Ruby.
- **Source-level fallback**: when manifests are absent, sparse, or ambiguous (e.g. a monorepo with multiple
  stacks, or a manifest that doesn't disambiguate framework choice), sample actual source files — file
  extensions, `import`/`using`/`#include` statements, and characteristic APIs — to confirm or fill the gap.
- **Antora documentation, if present** (per Step 4): read `antora.yml` (component `name`/`title`) and the docs
  pages themselves — an `installation.adoc`/`reference.adoc`/`getting-started.adoc` will typically spell out the
  language and build tool explicitly (e.g. a Maven/Gradle/npm/NuGet dependency snippet, a `pod` entry, an
  `import` example). Treat this as a corroborating signal, not a substitute for the manifest/source checks
  above — but when manifests are ambiguous or a module lacks one of its own, the docs are often the clearest
  statement of intent, and are worth quoting directly rather than re-deriving from source alone. Note any
  mismatch between what the docs claim and what the manifests/source actually show (e.g. stale docs describing a
  since-migrated framework) instead of silently preferring one over the other.
- **Multiple stacks**: if the repository contains more than one language/framework (e.g. a backend service plus
  a mobile client, or a monorepo of independent packages), record the stack per module/directory rather than
  flattening it into one list.

### Languages/frameworks implicated by the requested change

If a ticket was fetched, read its title/description/comments for explicit or implicit tech signals — named
languages, frameworks, or platforms (e.g. "iOS", "Android", "the web dashboard", "the API"), specific file/class
names, or code snippets whose syntax identifies a language. Then cross-reference against the codebase stack just
detected:

- If the ticket's signals match a part of the existing stack, note which module/language/framework the change
  targets.
- If the ticket names something **not** currently present in the codebase (e.g. a request to add a new mobile
  app to a backend-only repo, or to introduce a new framework), flag this explicitly as a new
  language/framework the work would introduce — don't silently assume it matches the existing stack.
- If nothing in the ticket indicates a specific target and the repo has a single stack, note that the change is
  presumed to apply to that stack. If the repo has multiple stacks and the ticket is genuinely ambiguous about
  which one it targets, say so rather than guessing.

If there is no ticket, this sub-step is just the codebase stack detected above — there is no requested change to
cross-reference yet.

## Step 6 — Report findings

Summarize for the user in plain text (no file output unless asked):

- Always include a **Tech stack** section, structured so later skills invoked in this same conversation (e.g.
  `plan`, `code`, code review, branch/PR creation) can read it directly instead of re-detecting it themselves:
  ```
  ## Tech stack
  - Languages (codebase): <e.g. Java, Kotlin>
  - Frameworks (codebase): <e.g. Spring Boot (backend/), Jetpack Compose (android/)>
  - Languages/frameworks (requested change): <e.g. Kotlin, Jetpack Compose — matches android/ module>
  - Antora docs: <e.g. docs/ (antora.yml component "my-lib") — or "none found">
  - Repository host: <e.g. GitHub (github.com/org/repo) — or Bitbucket / Azure DevOps / TFS, with base URL — or
    "no remote configured">
  ```
  If the repo has multiple modules/stacks, break the codebase lines out per module. If the requested change
  introduces something new (per Step 5), say so explicitly here rather than folding it silently into the
  existing list. Always state the Antora docs and Repository host lines, even when the answer is "none found"/"no
  remote", so later skills like `update-docs` or ones creating branches/PRs don't need to re-check.
- If a ticket was explored: restate the task in your own words, list the specific files/classes/methods
  relevant to it, explain how the current code behaves in the area the ticket touches, and flag anything
  ambiguous or missing from the ticket that would block implementation.
- If codebase-only: summarize the architecture areas covered and anything notable found (e.g. inconsistencies,
  undocumented behavior, missing test coverage) relevant to general orientation.
- Do not propose an implementation plan or start editing code — if the user wants that next, they will ask for
  it (e.g. via planning mode) as a separate step.
