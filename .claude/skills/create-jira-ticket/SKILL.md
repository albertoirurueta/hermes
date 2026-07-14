---
name: create-jira-ticket
description: Draft and file a new Jira ticket that is ready to be picked up by the `/issue`, `/plan`, `/code` flow. Runs the `explore` skill first to ground the ticket in the actual codebase, then asks the user for the purpose of the task to be implemented, then asks for additional context (linked URLs, attached files/documents, related tickets) and folds whatever is provided into the ticket description. Determines the target Jira project and issue type (Bug/Story/Task/etc.) from the stated purpose and this Jira instance's actual configuration, drafts a summary and description (Summary, Context, Acceptance criteria, References), and shows the draft for confirmation before filing it via connected Jira MCP tools. Invoke as `/create-jira-ticket` or `/create-jira-ticket <short description>`. Stops early if no Jira MCP tool is connected — there is no CLI fallback the way GitHub has `gh`. Use when the user has an idea, bug report, or request that needs to become a well-formed, actionable Jira ticket before `/issue` or `/plan` can pick it up — not for tickets that already exist (nothing to file) or when the work should be tracked as a GitHub issue instead (use `create-github-issue`).
---

# Create Jira Ticket

Turn a rough idea, bug report, or request into a well-formed Jira ticket — grounded in the real codebase, with a
clear purpose statement and whatever supporting context the user has — so it's immediately actionable by the
`/issue` or `/plan` skills. This skill only files a ticket; it does not branch, plan, or write code.

This is the Jira equivalent of `create-github-issue` — same shape, but sourced through Jira MCP tools instead of
the `gh` CLI, and with Jira's project/issue-type model instead of GitHub labels.

## Step 1 — Confirm Jira MCP tooling is available

Unlike GitHub (which has the `gh` CLI as a reliable fallback), there is no ubiquitous Jira CLI — this skill depends
entirely on a connected Jira MCP integration.

- Search for it: `ToolSearch` with a query like "jira issue" or "jira ticket".
- If one or more Jira MCP tools are found, note what they support (creating issues, listing projects, listing
  issue types, fetching issues by key) — later steps rely on whichever of these are actually available.
- If none are found, tell the user plainly that this environment has no Jira connection and this skill has no
  fallback for filing one without it, then stop. Suggest `create-github-issue` instead if the work should be
  tracked on GitHub.

## Step 2 — Explore the codebase

Run `Skill({skill: "explore"})` with no argument — there is no ticket yet, so this is a general orientation pass,
not one narrowed to a specific area. Skip this if general codebase exploration already happened earlier in this
same conversation and looks reasonably current; reuse it instead of repeating the work.

This grounds later steps in the real architecture, conventions, and tech stack (per `explore`'s own "Tech stack"
report) so the ticket can reference concrete files/modules instead of vague descriptions, and so its acceptance
criteria fit how the codebase actually behaves today.

## Step 3 — Ask for the purpose of the task

Ask the user directly, in plain conversational text (not `AskUserQuestion` — the answer is inherently open-ended,
not a choice among fixed options): what problem this ticket should solve or what capability it should add, why it
matters, and any acceptance criteria they already have in mind. If the skill was invoked with a short description
argument, treat that as a starting point and ask only for what it leaves unclear (e.g. the "why," or what "done"
looks like) rather than re-asking everything from scratch.

Wait for the user's reply before continuing — the rest of the ticket is drafted around this purpose.

## Step 4 — Ask for additional context

Ask the user (again as a plain, open-ended question) whether they have any of the following, making clear all of
it is optional:

- **Linked URL(s)** — design docs, discussions, external references, prior art.
- **Attached file(s) or documents** — screenshots, logs, specs, mockups (local paths).
- **Related ticket(s) or issue(s)** — in this Jira project, another project, or a linked GitHub repository, that
  this depends on, relates to, or duplicates.

Gather whatever is provided:

- **URLs**: fetch each with `WebFetch` and note the key points worth folding into the ticket — don't just link
  them blind if their content materially shapes scope or acceptance criteria.
- **Files/documents**: `Read` each local path (this tool also handles images and PDFs) and summarize what it
  shows that's relevant to the ticket.
- **Related Jira tickets**: confirm each with the connected Jira MCP tools (fetch by key: summary, status, URL) so
  the reference in the drafted description is accurate — don't take the user's key on faith without checking it
  resolves to what they mean. A related **GitHub** issue, if mentioned, can be confirmed with `gh issue view
  <id> --json number,title,url,state` if `gh` is available; otherwise just record the reference as given.

If the user has none of these, proceed with just the purpose from Step 3 — this step is not a blocker.

## Step 5 — Determine the target project and issue type

Jira tickets need a project and an issue type — unlike GitHub, where any issue lands in the one repository
already in context.

- **Project**: if the current conversation already established a Jira project (e.g. a prior `/explore PROJ-123`
  or `/issue PROJ-123` in this session), offer it as the default. Otherwise, if the Jira MCP tools support listing
  accessible projects, fetch that list and ask the user via `AskUserQuestion` to pick one (with a sensible default
  if only one project is accessible). If projects can't be listed, ask the user directly for the project key.
- **Issue type**: decide bug vs. feature/task, using the same signal words `issue`'s own classification step
  uses: language describing broken/incorrect existing behavior ("fails," "crash," "broken," "incorrect," "error
  when…") → `Bug`; language describing new, not-yet-existing capability ("add," "support for," "new,"
  "implement," "as a user I want…") → `Story`/`Task`/`New Feature` (whichever label this Jira instance uses).
  If the Jira MCP tools support listing a project's available issue types, fetch that list and match against it —
  don't invent an issue type name the project doesn't actually have configured. If the classification is
  genuinely ambiguous, or issue types can't be listed, ask the user via `AskUserQuestion` to pick.
- **Labels/components** (optional): if the user's stated purpose or additional context clearly implicates an
  existing label or component (check via the MCP tools if listing is supported), note it for Step 7; otherwise
  omit rather than guessing.

## Step 6 — Draft the ticket

Compose:

- **Summary**: a concise, imperative one-liner describing the change (e.g. "Add retry support to the HTTP
  client," "Fix list item move detection on drag cancel").
- **Description**, structured as:
  - **Summary** — the purpose and motivation from Step 3, in the user's own terms.
  - **Context** — grounded in Step 2's exploration: the relevant existing files/modules/classes and how they
    currently behave, so whoever picks this up (including a future `/explore`/`/plan` run) starts oriented
    instead of cold.
  - **Acceptance criteria** — a short bulleted list, if the user gave any in Step 3 or they can be reasonably
    inferred from the purpose; omit this section entirely rather than inventing criteria that weren't discussed.
  - **References** — any URLs, summarized file/document contents, and confirmed related-ticket/issue links from
    Step 4; omit if none were provided.

Show the full drafted summary, description, project, and issue type to the user before taking any action.

## Step 7 — Confirm and create

Filing a ticket is visible to everyone with access to this Jira project — never do it without explicit
confirmation. Ask the user via `AskUserQuestion`:

- **Create the ticket now** (recommended once the draft looks right).
- **Edit the draft first** — take their edits, update the draft, and re-ask.
- **Don't create it** — stop here; the draft itself is the deliverable.

If they confirm creation, use the connected Jira MCP tool(s) from Step 1 to create the ticket with the project,
issue type, summary, description, and any labels/components determined above. Capture the created ticket's key
and URL from the tool's response.

If ticket creation fails (e.g. a required field this Jira instance enforces wasn't supplied), surface the exact
error to the user rather than retrying blindly, and ask how to proceed (supply the missing field, or stop).

## Step 8 — Report

Give the user the ticket key and URL, and tell them the ticket is now ready to be picked up: `/issue <key>` to
kick off branch creation, exploration, planning, implementation, and PR creation in one pass, or `/plan <key>`
alone if they'd rather just get an implementation plan first.
