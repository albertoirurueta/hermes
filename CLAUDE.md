# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Hermes is a Java 17 library that detects changes between two versions of a collection (inserted, removed, updated,
and — for lists — moved items). It compares items using two independent notions: **identity** (do two items
represent the same logical entity, e.g. same id) and **content equality** (has that entity's data changed). It has
no runtime dependencies; only JUnit 5 and Mockito are used, in test scope.

## Commands

Run all tests:

```bash
mvn clean test
```

Run a single test class:

```bash
mvn test -Dtest=ListItemChangeDetectorTest
```

Run a single test method:

```bash
mvn test -Dtest=ListItemChangeDetectorTest#detectChanges_whenItemMoved_reportsMove
```

Full local verification (package, coverage, javadoc/source jars, Maven site with Checkstyle/SpotBugs/PMD/JaCoCo/
Surefire reports):

```bash
mvn clean jacoco:prepare-agent install jacoco:report javadoc:jar source:jar -P '!build-extras'
mvn site -Djacoco.skip -DskipTests -P '!build-extras'
```

Generated reports land under `target/site` (`apidocs/index.html`, `jacoco/index.html`, `surefire-report.html`).

Build the Antora documentation (requires Node.js 24+, only needed for docs work):

```bash
cd docs
npm install
npx antora antora-playbook.yml
```

Output goes to `docs/build/site`. Documentation pages live under `docs/modules/ROOT/pages`.

## Architecture

All source lives in a single package: `com.irurueta.hermes`. There are two parallel family trees of detectors that
share the same algorithmic shape but differ in what a "position" means and whether results are a `Collection` or an
ordered `List`:

1. **Collection detectors** (`AbstractCollectionItemChangeDetector`) — position-independent; only reports INSERTED,
   REMOVED, UPDATED. No move detection since there is no order to preserve.
2. **List detectors**, split into two positional semantics:
   - **Snapshot positions** (`AbstractListItemChangeDetector`, used by `ListItemChangeDetector` /
     `ComparableListItemChangeDetector`) — reported indices refer to the position in the old or new list as given,
     independent of the order changes are applied.
   - **Sequential edit positions** (`AbstractSequentialListItemChangeDetector`, used by
     `SequentialListItemChangeDetector` / `ComparableSequentialListItemChangeDetector`) — reported indices assume
     changes (remove, insert, move) are applied one after another to the old list, so each index reflects the list
     state *after* prior changes in the same detection run.

Each abstract base implements `detectChanges` as a fixed pipeline: copy both inputs defensively, then
`buildRemoves` → `buildInserts` → (`buildMoves` for lists) → `buildChanges`, accumulating typed change objects.
Concrete subclasses only supply three hooks: `hasContentChanged`, `notContains`, `indexOf`. There are two ways to
supply those hooks, giving each abstract detector two concrete subclasses:

- **Custom-comparator variant** (e.g. `CollectionItemChangeDetector`, `ListItemChangeDetector`,
  `SequentialListItemChangeDetector`) — takes an `ItemComparator<T>` (identity) and an `ItemContentComparator<T>`
  (content equality) in its constructor, for arbitrary item types.
- **`ComparableItem` variant** (e.g. `ComparableCollectionItemChangeDetector`, `ComparableListItemChangeDetector`,
  `ComparableSequentialListItemChangeDetector`) — for item types `T extends ComparableItem<T>` that implement
  `equals` (identity) and `equalContent` (content equality) themselves; no constructor arguments needed.

When adding a new detector variant, follow this pattern: extend the appropriate `Abstract*Detector`, implement the
three hooks, and add both a custom-comparator and a `ComparableItem` concrete class if the new positional/ordering
semantics warrants it.

Change results are modeled as small immutable value classes per action:
`Inserted/Removed/Updated(Collection|List)ItemChange` and, for lists only, `MovedListItemChange`. `CollectionItemChange`
and `ListItemChange` are the (non-generic) base types returned in the detection result collections/lists, exposing
just the `*ItemChangeAction` enum; callers downcast to the specific subtype to access item data and positions.

`BuildInfo` reads `build-info.properties`, generated at build time by the `groovy-maven-plugin` execution bound to
the `validate` phase in `pom.xml` (captures version, commit, branch, timestamp from CI env vars). Don't hand-edit
`src/main/resources/com/irurueta/hermes/build-info.properties` — it's regenerated on every Maven build.

## Conventions

- Public API methods validate arguments and throw `IllegalArgumentException` on null/invalid input rather than
  using assertions or optional wrappers.
- Use `final var` for locals throughout (see any existing class for style).
- Every public and protected member has Javadoc, including `@param`/`@return`/`@throws`; match this when adding
  API surface, since Checkstyle and the Javadoc plugin enforce documentation in the site build.
