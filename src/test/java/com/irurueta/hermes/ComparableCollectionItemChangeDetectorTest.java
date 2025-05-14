/*
 * Copyright (C) 2025 Alberto Irurueta Carro (alberto@irurueta.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irurueta.hermes;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ComparableCollectionItemChangeDetectorTest {

    private final ComparableCollectionItemChangeDetector<Item> detector =
            new ComparableCollectionItemChangeDetector<>();

    @Test
    void detectChange_whenNullNewItems_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> detector.detectChanges(null,
                Collections.emptyList()));
    }

    @Test
    void detectChange_whenNullOldItems_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> detector.detectChanges(Collections.emptyList(),
                null));
    }

    @Test
    void detectChange_whenEmpty_returnsEmpty() {
        final var oldlist = Collections.<Item>emptyList();
        final var newList = Collections.<Item>emptyList();

        final var changes = detector.detectChanges(newList, oldlist);

        assertTrue(changes.isEmpty());
    }

    @Test
    void detectChange_whenEqualLists_returnsEmpty() {
        final var list = List.of(new Item(1, "item1"));

        final var changes = detector.detectChanges(list, list);

        assertTrue(changes.isEmpty());
    }

    @Test
    void detectChanges_whenInsert_returnsExpectedResult() {
        final var oldList = Collections.<Item>emptyList();
        final var item = new Item(1, "item1");
        final var newList = List.of(item);

        final var changes = new ArrayList<>(detector.detectChanges(newList, oldList));

        assertEquals(1, changes.size());
        final var change = changes.get(0);
        assertEquals(CollectionItemChangeAction.INSERTED, change.getAction());
        final var insertedChange = assertInstanceOf(InsertedCollectionItemChange.class, change);
        assertSame(item, insertedChange.getNewItem());
    }

    @Test
    void detectChanges_whenInsertAtBeginning_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var oldList = List.of(item1);
        final var newList = List.of(item2, item1);

        final var changes = new ArrayList<>(detector.detectChanges(newList, oldList));

        assertEquals(1, changes.size());
        final var change = changes.get(0);
        assertEquals(CollectionItemChangeAction.INSERTED, change.getAction());
        final var insertedChange = assertInstanceOf(InsertedCollectionItemChange.class, change);
        assertSame(item2, insertedChange.getNewItem());
    }

    @Test
    void detectChanges_whenInsertAtEnd_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var oldList = List.of(item1);
        final var newList = List.of(item1, item2);

        final var changes = new ArrayList<>(detector.detectChanges(newList, oldList));

        assertEquals(1, changes.size());
        final var change = changes.get(0);
        assertEquals(CollectionItemChangeAction.INSERTED, change.getAction());
        final var insertedChange = assertInstanceOf(InsertedCollectionItemChange.class, change);
        assertSame(item2, insertedChange.getNewItem());
    }

    @Test
    void detectChanges_whenMultipleInserts_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var item3 = new Item(3, "item3");

        final var oldList = List.of(item1);
        final var newList = List.of(item3, item1, item2);

        final var changes = new ArrayList<>(detector.detectChanges(newList, oldList));

        assertEquals(2, changes.size());

        final var change1 = changes.get(0);
        assertEquals(CollectionItemChangeAction.INSERTED, change1.getAction());
        final var insertedChange1 = assertInstanceOf(InsertedCollectionItemChange.class, change1);
        assertSame(item3, insertedChange1.getNewItem());

        final var change2 = changes.get(1);
        assertEquals(CollectionItemChangeAction.INSERTED, change2.getAction());
        final var insertedChange2 = assertInstanceOf(InsertedCollectionItemChange.class, change2);
        assertSame(item2, insertedChange2.getNewItem());
    }

    @Test
    void detectChanges_whenRemove_returnsExpectedResult() {
        final var item = new Item(1, "item1");
        final var oldList = List.of(item);
        final var newList = Collections.<Item>emptyList();

        final var changes = new ArrayList<>(detector.detectChanges(newList, oldList));

        assertEquals(1, changes.size());
        final var change = changes.get(0);
        assertEquals(CollectionItemChangeAction.REMOVED, change.getAction());
        final var removedChange = assertInstanceOf(RemovedCollectionItemChange.class, change);
        assertSame(item, removedChange.getOldItem());
    }

    @Test
    void detectChanges_whenRemoveAtBeginning_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var oldList = List.of(item2, item1);
        final var newList = List.of(item1);

        final var changes = new ArrayList<>(detector.detectChanges(newList, oldList));

        assertEquals(1, changes.size());
        final var change = changes.get(0);
        assertEquals(CollectionItemChangeAction.REMOVED, change.getAction());
        final var removedChange = assertInstanceOf(RemovedCollectionItemChange.class, change);
        assertSame(item2, removedChange.getOldItem());
    }

    @Test
    void detectChanges_whenRemoveAtEnd_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var oldList = List.of(item1, item2);
        final var newList = List.of(item1);

        final var changes = new ArrayList<>(detector.detectChanges(newList, oldList));

        assertEquals(1, changes.size());
        final var change = changes.get(0);
        assertEquals(CollectionItemChangeAction.REMOVED, change.getAction());
        final var removedChange = assertInstanceOf(RemovedCollectionItemChange.class, change);
        assertSame(item2, removedChange.getOldItem());
    }

    @Test
    void detectChanges_whenMultipleRemoves_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var item3 = new Item(3, "item3");

        final var oldList = List.of(item3, item1, item2);
        final var newList = List.of(item1);

        final var changes = new ArrayList<>(detector.detectChanges(newList, oldList));

        assertEquals(2, changes.size());

        final var change1 = changes.get(0);
        assertEquals(CollectionItemChangeAction.REMOVED, change1.getAction());
        final var removedChange1 = assertInstanceOf(RemovedCollectionItemChange.class, change1);
        assertSame(item3, removedChange1.getOldItem());

        final var change2 = changes.get(1);
        assertEquals(CollectionItemChangeAction.REMOVED, change2.getAction());
        final var removedChange2 = assertInstanceOf(RemovedCollectionItemChange.class, change2);
        assertSame(item2, removedChange2.getOldItem());
    }

    @Test
    void detectChanges_whenSwap_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var oldList = List.of(item1, item2);
        final var newList = List.of(item2, item1);

        final var changes = detector.detectChanges(newList, oldList);

        assertTrue(changes.isEmpty());
    }

    @Test
    void detectChanges_whenReversed_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var item3 = new Item(3, "item3");
        final var oldList = List.of(item1, item2, item3);
        final var newList = List.of(item3, item2, item1);

        final var changes = detector.detectChanges(newList, oldList);

        assertTrue(changes.isEmpty());
    }

    @Test
    void detectChanges_whenUpdate_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var item2b = new Item(2, "item2b");

        final var oldList = List.of(item1, item2);
        final var newList = List.of(item1, item2b);

        final var changes = new ArrayList<>(detector.detectChanges(newList, oldList));

        assertEquals(1, changes.size());

        final var change = changes.get(0);
        assertEquals(CollectionItemChangeAction.UPDATED, change.getAction());
        final var updateChange = assertInstanceOf(UpdatedCollectionItemChange.class, change);
        assertSame(item2, updateChange.getOldItem());
        assertSame(item2b, updateChange.getNewItem());
    }

    @Test
    void detectChanges_whenMultipleActions_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var item2b = new Item(2, "item2b");
        final var item3 = new Item(3, "item3");
        final var item4 = new Item(4, "item4");
        final var item5 = new Item(5, "item5");
        final var oldList = List.of(item1, item2, item3, item4);
        final var newList = List.of(item3, item2b, item1, item5);

        final var changes = new ArrayList<>(detector.detectChanges(newList, oldList));

        assertEquals(3, changes.size());

        final var change1 = changes.get(0);
        assertEquals(CollectionItemChangeAction.REMOVED, change1.getAction());
        final var removedChange = assertInstanceOf(RemovedCollectionItemChange.class, change1);
        assertSame(item4, removedChange.getOldItem());

        final var change2 = changes.get(1);
        assertEquals(CollectionItemChangeAction.INSERTED, change2.getAction());
        final var insertedChange = assertInstanceOf(InsertedCollectionItemChange.class, change2);
        assertSame(item5, insertedChange.getNewItem());

        final var change3 = changes.get(2);
        assertEquals(CollectionItemChangeAction.UPDATED, change3.getAction());
        final var updatedChange = assertInstanceOf(UpdatedCollectionItemChange.class, change3);
        assertSame(item2, updatedChange.getOldItem());
        assertSame(item2b, updatedChange.getNewItem());
    }

    private record Item(int id, String content) implements ComparableItem<Item> {

        @Override
        public boolean equals(final Object other) {
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            final var item = (Item) other;
            return id == item.id;
        }

        @Override
        public boolean equalContent(final Item item) {
            return Objects.equals(content, item.content);
        }
    }
}