# Hermes
A library to detect changes on collection items

Hermes is the Greek god of commerce, travelers, communication, and is also associated with the exchange of information. This name evokes 
the idea of a fast and efficient messenger, which reflects the library’s function of detecting changes and updates in collections swiftly 
and accurately.

[![Build Status](https://github.com/albertoirurueta/hermes/actions/workflows/master.yml/badge.svg)](https://github.com/albertoirurueta/hermes/actions)
[![Build Status](https://github.com/albertoirurueta/hermes/actions/workflows/develop.yml/badge.svg)](https://github.com/albertoirurueta/hermes/actions)

[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_hermes&metric=bugs)](https://sonarcloud.io/dashboard?id=albertoirurueta_hermes)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_hermes&metric=code_smells)](https://sonarcloud.io/dashboard?id=albertoirurueta_hermes)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_hermes&metric=coverage)](https://sonarcloud.io/dashboard?id=albertoirurueta_hermes)

[![Duplicated lines](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_hermes&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=albertoirurueta_hermes)
[![Lines of code](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_hermes&metric=ncloc)](https://sonarcloud.io/dashboard?id=albertoirurueta_hermes)

[![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_hermes&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=albertoirurueta_hermes)
[![Quality gate](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_hermes&metric=alert_status)](https://sonarcloud.io/dashboard?id=albertoirurueta_hermes)
[![Reliability](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_hermes&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=albertoirurueta_hermes)

[![Security](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_hermes&metric=security_rating)](https://sonarcloud.io/dashboard?id=albertoirurueta_hermes)
[![Technical debt](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_hermes&metric=sqale_index)](https://sonarcloud.io/dashboard?id=albertoirurueta_hermes)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_hermes&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=albertoirurueta_hermes)

[Maven Site Report](http://albertoirurueta.github.io/hermes)

## Usage

Add the following dependency to your project:

Latest release:
```
<dependency>
    <groupId>com.irurueta</groupId>
    <artifactId>hermes</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
```

Latest snapshot:
```
<dependency>
    <groupId>com.irurueta</groupId>
    <artifactId>hermes</artifactId>
    <version>1.1.0-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
```

To use this library, you must take into account:
* The collections being compared are position-aware or not (list vs. sets, maps, etc.)
* The items in your collection implement the ComparableItem interface or not.
* For position-aware collections you want changes position values considering that changes are applied sequentially or 
  not.

Based on those three factors, the following classes should be used:
* **CollectionItemChangeDetector**: when comparing non-position-aware collections and collection items do NOT
  implement any specific interface. (proper comparators must be provided).
* **ComparableCollectionItemChangeDetector**: when comparing non-position-aware collections and collection items
  implement ComparableItem interface.
* **ListItemChangeDetector**: when comparing position-aware collections and collection items do NOT implement any
  specific interface. (proper comparators must be provided).
* **ComparableListItemChangeDetector**: when comparing position-aware collections and collection items, implement
  ComparableItem interface.
* **SequentialListItemChangeDetector**: when comparing position-aware collections, collection items do NOT implement
  any specific interface, and returned changes (and their positions) are assumed to be applied sequentially.
* **SequentialComparableListItemChangeDetector**: when comparing position-aware collections, collection items implement
  ComparableItem interface, and returned changes (and their positions) are assumed to be applied sequentially.

## Example

```
// Define a class to be used as item in the lists to be compared
public record Item(int id, String content) { }

...

    // Create a detector
    final var detector = new ListItemChangeDetector<Item>(
            (item1, item2) -> item1.id() == item2.id(),
            (item1, item2) -> Objects.equals(item1.content(), item2.content()));
            
    // Create two lists to be compared
    final var item1 = new Item(1, "item1");
    final var item2 = new Item(2, "item2");
    final var item2b = new Item(2, "item2b");
    final var item3 = new Item(3, "item3");
    final var item4 = new Item(4, "item4");
    final var item5 = new Item(5, "item5");
    final var oldList = List.of(item1, item2, item3, item4);
    final var newList = List.of(item3, item2b, item1, item5);
            
    // Detect changes between the two lists
    final var changes = detector.detectChanges(newList, oldList);            
```

The execution of the previous code will return a list of changes with the following values:

* Change 1:
  * ChangeType: REMOVED
  * Position: 3
  * Explanation: Item 4 was removed from the list.

* Change 2:
  * ChangeType: INSERTED
  * Position: 3
  * Explanation: Item 5 was inserted in position 3.

* Change 3:
  * ChangeType: MOVED
  * Old Position: 0
  * New Position: 2
  * Explanation: Item 1 was moved from position 0 to position 2.

* Change 4:
  * ChangeType: MOVED
  * Old Position: 2
  * New Position: 0
  * Explanation: Item 3 was moved from position 2 to position 0.

* Change 5:
  * ChangeType: UPDATED
  * Position: 1
  * Explanation: Item 2 was updated.