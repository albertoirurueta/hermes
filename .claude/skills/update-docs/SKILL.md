---
name: update-docs
description: Update a repository's Antora AsciiDoc documentation to reflect changes already made to the codebase (new/changed APIs, types, behavior, or conventions). Invoke as `/update-docs` to cover uncommitted changes plus commits on the current branch not yet on the base branch, or `/update-docs <git-ref-or-range>` to scope it to a specific ref/range (e.g. a commit, `main..HEAD`, a tag). Diagrams use Mermaid (`[mermaid]` blocks) and equations use MathJax (raw `\( \)` / `\[ \]` LaTeX) where they clarify behavior and the docs pipeline already supports them. Use whenever source changes should be reflected in the docs site instead of leaving pages stale.
---

# Update Docs

Bring an Antora-based documentation module's `.adoc` pages (and its `nav.adoc`/`antora.yml` if structure or
version changed) up to date with changes already made to the codebase. This skill only edits documentation — it
does not change source code, and it does not invent new documented behavior that isn't actually in the code. It
makes no assumptions about this being any particular repository, project, or language — discover everything
about the docs module and the codebase fresh each run.

## Step 1 — Determine the diff to document

- **Argument provided** (a commit, ref, or range like `main..HEAD`): use `git diff <arg>` as the scope.
- **No argument**: default to everything not yet merged plus anything still dirty:
  ```bash
  git status
  git diff <base-branch>...HEAD
  git diff
  ```
  (determine the repo's actual base branch first — e.g. `git symbolic-ref refs/remotes/origin/HEAD` or
  `git branch -a`; ask the user if it's genuinely ambiguous).
- If the resulting diff is empty (no local changes, branch even with base), tell the user there is nothing to
  document and stop rather than guessing at unrelated updates.
- Read the full diff, not just file names — the doc updates must be grounded in what actually changed, not in
  file names alone.

## Step 2 — Locate the documentation module

Don't assume a fixed docs path. Find the Antora module(s) actually present, typically by locating `antora.yml`
file(s) in the repository (`find . -name antora.yml -not -path '*/node_modules/*'`).

- **No `antora.yml` found anywhere**: there is no documentation module yet to update. Ask the user whether Antora
  documentation should be set up now (via the `antora-setup` skill) before continuing — don't assume either way.
  - **User agrees**: delegate scaffolding to the `antora-setup` skill, run as a sub-agent via the Agent tool.
    Brief that sub-agent with the actual context (what changed, per Step 1/3) so its starter pages aren't generic
    boilerplate disconnected from this update. After it finishes, re-run the `find . -name antora.yml ...` search
    to pick up the newly created module and proceed with the rest of this step as normal. If the sub-agent
    reports it stopped short (e.g. Node.js isn't installed, per its own Step 1) and therefore no module was
    created, relay that blocker to the user and stop rather than continuing without docs.
  - **User declines**: stop the `update-docs` skill entirely — with no documentation module, there is nothing
    for it to update, so don't fall back to any other action.
- **One or more found**: for each one found:

- Read it to get the component name, version, and the nav file(s) it declares under `nav:`.
- Read each declared `nav.adoc` to get the real page list, then skim each listed page (title + first section is
  usually enough) to build a live map of what topic each page actually covers. Don't rely on page names alone —
  a page called `overview.adoc` in one repo might be called `index.adoc` or `intro.adoc` in another.
- Note the antora playbook file (commonly `antora-playbook.yml` or `site.yml`, usually near the docs module root
  or repository root) and read it once to check which Asciidoctor/Antora extensions are already wired up (e.g. a
  Mermaid extension, a MathJax/stem extension, `lunr` search) — this determines what's actually safe to use in
  Step 4, since a diagram/equation macro with no matching extension configured just renders as literal text.

## Step 3 — Understand what changed, in code terms

For every changed source file, determine in concrete terms, adapting to whatever language/stack this repository
actually uses:

- New or removed public classes/interfaces/functions/exports/endpoints, and any renamed ones.
- Behavioral changes to existing abstractions, especially ones already described in the docs (check the page
  map from Step 2).
- New variants/subtypes of an existing abstraction — these are the changes most likely to need a new branch in
  an existing decision tree/diagram or a new row in an existing comparison table.
- Changes to build/tooling that affect documented commands (e.g. a version bump, a new build profile, a new
  generated report, a changed CLI flag).

Cross-check against the repository's own contributor guide, if one exists (`CLAUDE.md`, `AGENTS.md`, or a
top-level `README`) so the docs stay consistent with it; if the diff changes something that guide also
describes, flag that to the user as a possible follow-up — this skill only edits the docs module, not that
guide.

## Step 4 — Map changes to affected pages, then edit them

Using the live page map built in Step 2 (not any assumption about fixed page names), decide which pages the
diff actually affects — don't touch pages the diff doesn't concern. A new abstraction variant typically touches
whichever page holds its decision tree/comparison table plus any page listing the top-level API surface. A
behavior-only fix with no API/semantics change usually needs no doc update at all — say so if that's the case
instead of forcing an edit. Only touch `nav.adoc`/`antora.yml` if a page was added/removed/renamed or the
component version should track a source version bump.

When editing:

- Match the conventions already used in that module's existing pages — read a couple of them first to learn the
  local section-title style, table syntax (`[cols=...]`), code-block language tags, and cross-link style
  (`xref:page.adoc[]`) rather than assuming AsciiDoc defaults or another project's conventions.
- Verify every class/method/field/endpoint named in prose or code blocks actually exists with that exact
  signature — read the real source rather than trusting the diff summary alone. Code snippets must be valid
  against the current API.
- **Mermaid diagrams** (`[mermaid]` fenced blocks): only use these if Step 2 confirmed a Mermaid extension is
  already wired into the playbook. Add one when a decision process, data flow, or before/after transformation is
  genuinely clearer as a diagram than prose — don't add one for something a short sentence already covers
  clearly, and don't introduce the syntax at all if the extension isn't configured (it would render as literal
  text) unless you also wire up the extension or explicitly flag the gap to the user.
- **MathJax equations** (raw LaTeX delimited by `\( ... \)` inline or `\[ ... \]` block, rendered client-side):
  only use these if Step 2 confirmed a MathJax/stem extension is wired in. Add one only if the change actually
  introduces math worth stating precisely (e.g. a complexity bound or a formal definition); most changes won't
  need one, so don't force one in.
- If a change needs an entirely new page, create it in the same directory as the module's other pages, add an
  `xref:` entry to the relevant `nav.adoc` in a sensible position, and link to/from related existing pages.
- Preserve any existing disclaimers/notices (e.g. an AI-generated-content notice) already present on a page
  unless the change specifically concerns them.

## Step 5 — Verify the site still builds

If a working Node.js toolchain is available and the playbook found in Step 2 is an Antora playbook:

```bash
cd <docs-module-directory>
npm install
npx antora <playbook-file>
```

Report any Antora/Asciidoctor errors (broken `xref:`, malformed tables, bad Mermaid/MathJax syntax) and fix them
before finishing. If Node isn't available or the build can't be run, say so explicitly and note the pages were
updated but not locally verified against a real build.

## Step 6 — Report

Summarize, per page touched: what changed in the source that drove the edit, and what was updated in the docs.
Explicitly call out pages you considered but left unchanged because the diff didn't affect their content, and
any doc gaps you noticed but didn't fix because they were outside the scope of the diff being documented.

End the report by reminding the user to check the generated documentation themselves before trusting it — point
them at the built site's start page under the module's build output directory (from Step 5, if the build ran),
since rendered Mermaid diagrams, MathJax equations, and cross-page links can look fine in source but render
wrong, and this skill's own build check does not visually inspect the output.
