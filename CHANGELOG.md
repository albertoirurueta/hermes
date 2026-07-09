# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.3.0] - 2026-06-29

### Added

- Antora documentation site, published to GitHub Pages alongside the Maven site report.
- SonarCloud analysis wired into the `develop` and release CI builds.

## [1.2.0] - 2025-09-17

### Changed

- Maven build and CI workflow updates (build plugins, GitHub Actions).

## [1.1.0] - 2025-09-14

### Changed

- Javadoc refinements.

## [1.0.0] - 2025-05-14

### Added

- Initial release: detect inserted, removed, updated, and (for lists) moved items between two versions of a
  collection.
- Collection detectors (`CollectionItemChangeDetector`, `ComparableCollectionItemChangeDetector`) for
  position-independent collections.
- List detectors with snapshot positions (`ListItemChangeDetector`, `ComparableListItemChangeDetector`), where
  reported indices refer to the original old/new list.
- List detectors with sequential edit positions (`SequentialListItemChangeDetector`,
  `ComparableSequentialListItemChangeDetector`), where reported indices assume changes are applied one after
  another.
- `ComparableItem` interface for item types that implement identity and content equality themselves, as an
  alternative to supplying custom `ItemComparator`/`ItemContentComparator` instances.

[Unreleased]: https://github.com/albertoirurueta/hermes/compare/1.3.0...HEAD
[1.3.0]: https://github.com/albertoirurueta/hermes/compare/1.2.0...1.3.0
[1.2.0]: https://github.com/albertoirurueta/hermes/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/albertoirurueta/hermes/compare/1.0.0...1.1.0
[1.0.0]: https://github.com/albertoirurueta/hermes/releases/tag/1.0.0
