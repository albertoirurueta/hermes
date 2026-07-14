---
name: plan
description: Generate an implementation_plan.md at the repository root detailing the tasks needed to complete a piece of work, optionally grounded in a specific GitHub issue. Invoke as `/plan <issue-id>` to plan for that issue, or `/plan` with a description in the same message to plan manually described work. Runs the `explore` skill first to ground the plan in the actual codebase and issue, unless that exploration already happened earlier in this conversation. Records which language/framework (e.g. `java`, `dotnet`) each task belongs to — once up front if uniform across the whole plan, or per top-level task if the plan spans more than one — using the same keys as this repository's installed `<language>-code-one-task` skills, so a downstream execution skill knows which one to invoke per task without re-inferring it. Use when the user wants a concrete, reviewable step-by-step plan before code changes begin.
---

# Plan

Produce a concrete, step-by-step implementation plan for a task — grounded in the real state of the codebase —
and write it to `implementation_plan.md` at the repository root. This skill does not modify source code; its
only output artifact is the plan file itself.

## Step 1 — Determine what is being planned

The skill may be invoked with a GitHub issue ID as its argument (e.g. `/plan 42`), or with a free-form
description of work to plan (e.g. `/plan add retry support to the HTTP client`), or with neither.

- **Issue ID provided**: use it as the issue ID, go to Step 2.
- **Free-form task description provided** (in the invocation or immediately preceding user messages): use that
  as the task definition, go to Step 3 (no issue to fetch).
- **Neither provided**: ask the user (via `AskUserQuestion`) whether they want to plan around a GitHub issue ID
  or a manually described task. If they give neither, ask them to briefly describe the task — a plan cannot be
  produced without knowing what to plan.

## Step 2 — Ground the plan via the `explore` skill

If an issue ID is involved, or the task otherwise warrants understanding unfamiliar parts of the codebase, this
plan must be grounded in a prior exploration, not produced from a cold start.

- **Check first**: look back in the current conversation for exploration already performed for this same issue
  ID or task (either via the `explore` skill or equivalent research already done in this session). If that
  exploration is present and looks reasonably current, reuse it and skip straight to Step 3 — do not repeat it.
- **Otherwise**: invoke the `explore` skill via the `Skill` tool, passing the issue ID as its argument if one
  exists (`Skill({skill: "explore", args: "<issue-id>"})`), or with no argument if this is a manually described
  task that still needs codebase orientation. Wait for it to complete before continuing.
- If the task is trivial and self-contained enough that no codebase orientation is needed at all (e.g. the user
  already pasted all relevant context, or the change is confined to a single already-known file), exploration
  may be skipped — use judgment, but default to exploring when unsure.

## Step 3 — Resolve ambiguity, but only when necessary

After grounding, decide whether anything is genuinely ambiguous or under-specified in a way that would make the
plan wrong or force a guess with real consequences (e.g. conflicting requirements, a choice between materially
different architectures, a missing decision only the user can make like which library/framework to standardize
on, or scope that could reasonably mean two very different things).

- If something is ambiguous: ask the user via `AskUserQuestion`, but keep it tight — 1-4 focused questions,
  each with a recommended default option based on codebase conventions and common best practices for the
  language/framework/architecture in play. Do not ask about things you can reasonably infer or that have an
  obvious best-practice answer.
- If nothing is genuinely ambiguous: do **not** ask the user anything. Proceed straight to drafting the plan,
  choosing the best-practice approach yourself given the language, frameworks, libraries, and existing
  architectural conventions found in Step 2 (check the repository's own `CLAUDE.md` or equivalent contributor
  docs, if present, for conventions to follow). State the approach you chose and why in the plan itself so the
  user can see and challenge the reasoning during review, rather than being asked upfront.

## Step 4 — Determine the language/framework key(s) for downstream execution

Downstream, a `code`-style skill executes this plan one task at a time by invoking a language-specific
`<key>-code-one-task` skill per task (e.g. `java-code-one-task`, `dotnet-code-one-task`). That dispatch needs an
explicit, unambiguous key per task — don't leave it to be re-inferred later from file extensions or prose.

- Find which `*-code-one-task` skills are actually installed in this repository:
  `find .claude/skills -maxdepth 1 -type d -name "*-code-one-task"`. The directory name minus the
  `-code-one-task` suffix is the key (e.g. `java-code-one-task` → `java`, `dotnet-code-one-task` → `dotnet`). Use
  these exact keys — don't invent a synonym (e.g. write `dotnet`, not `csharp` or `net`, if `dotnet-code-one-task`
  is what's installed).
- Determine each task's language/framework from the exploration in Step 2 (the `explore` skill already detects the
  language(s)/framework(s) in play) and from the concrete file(s) each task names.
- If a task's actual language/framework has no matching `*-code-one-task` skill installed, still record the real
  language/framework (e.g. `python`, `typescript`) rather than forcing it into an unrelated key — a downstream
  skill can then fall back to generic tooling or flag it as unsupported, but the plan must stay accurate about
  what the task actually is.
- If a task's language/framework genuinely can't be determined (e.g. a non-code task like documentation-only or
  config-only changes, or exploration simply didn't surface enough to tell), or it's determined but has no
  matching `*-code-one-task` skill installed and isn't a recognizable language/framework worth naming, do **not**
  block on it or guess — still add the task to the plan, just without any language/framework tag or line for it.
  A missing tag is a valid, expected state; a downstream skill treats an untagged task as one it must implement
  directly rather than dispatch to a language-specific skill.
- **Every task shares one language/framework** (the common case — most repositories, most plans): note it once,
  it applies to the whole plan.
- **Tasks span more than one** (e.g. a change touching both a Java backend and a .NET client, or a polyglot
  monorepo): note it per top-level task instead, since each may dispatch to a different execution skill.

## Step 5 — Draft `implementation_plan.md`

Write the plan to a file named `implementation_plan.md` at the repository root (overwrite it if it already
exists — plans are meant to be regenerated; if there is reason to believe the existing file holds in-progress
work the user cares about, check with the user before overwriting it). Structure it as:

1. **Task summary** — a brief, plain-language restatement of what is being requested and why (from the issue
   and/or user's description). If choices were made on the user's behalf per Step 3, state them here with a
   one-line rationale. If Step 4 found every task shares one language/framework, state it here as a
   **Language/framework** line, e.g. `**Language/framework:** dotnet (.NET / C#)` or `**Language/framework:** java
   (Maven)` — the key first (matching an installed `<key>-code-one-task` skill, or the plain language name if none
   is installed), then a human-readable label in parentheses.
2. **Current code state** — a brief summary of the relevant existing architecture/classes/files as found during
   exploration: what exists today, how it is structured, and where the change needs to land. Reference concrete
   file paths and class/method names from the actual repository, not vague descriptions.
3. **Implementation steps** — a numbered list of tasks, each broken into sub-tasks as needed. Every task and
   sub-task must be explicit and actionable, not aspirational:
   - Name the exact file(s) to create or modify.
   - Describe the exact change (new class/function/field, signature, behavior, which hook/interface it
     implements, what test to add, what documentation is required, etc.).
   - Where it clarifies the intent, include a short illustrative code example (a signature, a snippet, a test
     case sketch) — enough to remove guesswork, not a full implementation.
   - Order tasks so each one is buildable/testable on top of the previous (e.g. add the abstraction before the
     concrete implementation that uses it; add the implementation before the tests that exercise it).
   - Call out verification steps explicitly where this repository's conventions require them (e.g. "update
     the docs", "run the linter/static-analysis profile"). For running the tests touched by a task, call out
     delegating to a sub-agent in a separate context (e.g. `Agent({description: "Run tests for <selector>",
     prompt: "Invoke Skill({skill: \"<test-skill>\", args: \"<selector>\"}) ..."})`) rather than running the
     test-running skill or command directly in the main conversation, so test output doesn't consume the main
     context window. Scope it to the relevant test(s) using a repository-specific test-running skill if one is
     available in this repository; otherwise have the sub-agent run the equivalent test-runner command for this
     repository's language/build tool directly.
   - Give every task and every sub-task an empty markdown checkbox placeholder, e.g. `1. [ ] **Some task**` for a 
     top-level task and `- [ ] Some subtask` for a sub-task nested under it. The `code` skill checks these off as it 
     completes each one, so every task/sub-task line needs its own `[ ]` — don't share one checkbox across several 
     sub-tasks or omit it from sub-tasks in favor of only checking the parent.
   - If Step 4 found the plan spans more than one language/framework, tag each top-level task's line with its key
     right after the bold title, e.g. `1. [ ] **Some task** _(dotnet)_` — a sub-task inherits its parent task's
     language/framework and doesn't need its own tag. Omit this tag entirely when the plan-wide
     **Language/framework** line in the Task summary already covers every task.

Keep the plan concrete and skimmable: prefer nested numbered/bulleted lists over prose paragraphs.

## Step 6 — Ask for review

Once `implementation_plan.md` is written, tell the user it's ready, give a one- or two-sentence summary of the
approach, and explicitly ask them to review the file and let you know if anything needs to change before
implementation starts. Do not begin implementing the plan unless the user asks you to.
