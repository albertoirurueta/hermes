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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ComparableSequentialListItemChangeDetectorTest {

    private final ComparableSequentialListItemChangeDetector<Item> detector =
            new ComparableSequentialListItemChangeDetector<>();

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

        final var changes = detector.detectChanges(newList, oldList);

        assertEquals(1, changes.size());
        final var change = changes.get(0);
        assertEquals(ListItemChangeAction.INSERTED, change.getAction());
        final var insertedChange = assertInstanceOf(InsertedListItemChange.class, change);
        assertSame(item, insertedChange.getNewItem());
        assertEquals(0, insertedChange.getNewPosition());
    }

    @Test
    void detectChanges_whenInsertAtBeginning_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var oldList = List.of(item1);
        final var newList = List.of(item2, item1);

        final var changes = detector.detectChanges(newList, oldList);

        assertEquals(1, changes.size());
        final var change = changes.get(0);
        assertEquals(ListItemChangeAction.INSERTED, change.getAction());
        final var insertedChange = assertInstanceOf(InsertedListItemChange.class, change);
        assertSame(item2, insertedChange.getNewItem());
        assertEquals(0, insertedChange.getNewPosition());
    }

    @Test
    void detectChanges_whenInsertAtEnd_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var oldList = List.of(item1);
        final var newList = List.of(item1, item2);

        final var changes = detector.detectChanges(newList, oldList);

        assertEquals(1, changes.size());
        final var change = changes.get(0);
        assertEquals(ListItemChangeAction.INSERTED, change.getAction());
        final var insertedChange = assertInstanceOf(InsertedListItemChange.class, change);
        assertSame(item2, insertedChange.getNewItem());
        assertEquals(1, insertedChange.getNewPosition());
    }

    @Test
    void detectChanges_whenMultipleInserts_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var item3 = new Item(3, "item3");

        final var oldList = List.of(item1);
        final var newList = List.of(item3, item1, item2);

        final var changes = detector.detectChanges(newList, oldList);

        assertEquals(2, changes.size());

        final var change1 = changes.get(0);
        assertEquals(ListItemChangeAction.INSERTED, change1.getAction());
        final var insertedChange1 = assertInstanceOf(InsertedListItemChange.class, change1);
        assertSame(item3, insertedChange1.getNewItem());
        assertEquals(0, insertedChange1.getNewPosition());

        final var change2 = changes.get(1);
        assertEquals(ListItemChangeAction.INSERTED, change2.getAction());
        final var insertedChange2 = assertInstanceOf(InsertedListItemChange.class, change2);
        assertSame(item2, insertedChange2.getNewItem());
        assertEquals(2, insertedChange2.getNewPosition());
    }

    @Test
    void detectChanges_whenRemove_returnsExpectedResult() {
        final var item = new Item(1, "item1");
        final var oldList = List.of(item);
        final var newList = Collections.<Item>emptyList();

        final var changes = detector.detectChanges(newList, oldList);

        assertEquals(1, changes.size());
        final var change = changes.get(0);
        assertEquals(ListItemChangeAction.REMOVED, change.getAction());
        final var removedChange = assertInstanceOf(RemovedListItemChange.class, change);
        assertSame(item, removedChange.getOldItem());
        assertEquals(0, removedChange.getOldPosition());
    }

    @Test
    void detectChanges_whenRemoveAtBeginning_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var oldList = List.of(item2, item1);
        final var newList = List.of(item1);

        final var changes = detector.detectChanges(newList, oldList);

        assertEquals(1, changes.size());
        final var change = changes.get(0);
        assertEquals(ListItemChangeAction.REMOVED, change.getAction());
        final var removedChange = assertInstanceOf(RemovedListItemChange.class, change);
        assertSame(item2, removedChange.getOldItem());
        assertEquals(0, removedChange.getOldPosition());
    }

    @Test
    void detectChanges_whenRemoveAtEnd_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var oldList = List.of(item1, item2);
        final var newList = List.of(item1);

        final var changes = detector.detectChanges(newList, oldList);

        assertEquals(1, changes.size());
        final var change = changes.get(0);
        assertEquals(ListItemChangeAction.REMOVED, change.getAction());
        final var removedChange = assertInstanceOf(RemovedListItemChange.class, change);
        assertSame(item2, removedChange.getOldItem());
        assertEquals(1, removedChange.getOldPosition());
    }

    @Test
    void detectChanges_whenMultipleRemoves_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var item3 = new Item(3, "item3");

        final var oldList = List.of(item3, item1, item2);
        final var newList = List.of(item1);

        final var changes = detector.detectChanges(newList, oldList);

        assertEquals(2, changes.size());

        final var change1 = changes.get(0);
        assertEquals(ListItemChangeAction.REMOVED, change1.getAction());
        final var removedChange1 = assertInstanceOf(RemovedListItemChange.class, change1);
        assertSame(item3, removedChange1.getOldItem());
        assertEquals(0, removedChange1.getOldPosition());

        final var change2 = changes.get(1);
        assertEquals(ListItemChangeAction.REMOVED, change2.getAction());
        final var removedChange2 = assertInstanceOf(RemovedListItemChange.class, change2);
        assertSame(item2, removedChange2.getOldItem());
        assertEquals(1, removedChange2.getOldPosition());
    }

    @Test
    void detectChanges_whenSwap_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var oldList = List.of(item1, item2);
        final var newList = List.of(item2, item1);

        final var changes = detector.detectChanges(newList, oldList);

        assertEquals(1, changes.size());

        final var change = changes.get(0);
        assertEquals(ListItemChangeAction.MOVED, change.getAction());
        final var moveChange = assertInstanceOf(MovedListItemChange.class, change);
        assertSame(item1, moveChange.getOldItem());
        assertSame(item1, moveChange.getNewItem());
        assertEquals(0, moveChange.getOldPosition());
        assertEquals(1, moveChange.getNewPosition());
    }

    @Test
    void detectChanges_whenReversed_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var item3 = new Item(3, "item3");
        final var oldList = List.of(item1, item2, item3);
        final var newList = List.of(item3, item2, item1);

        final var changes = detector.detectChanges(newList, oldList);

        assertEquals(2, changes.size());

        final var change1 = changes.get(0);
        assertEquals(ListItemChangeAction.MOVED, change1.getAction());
        final var moveChange1 = assertInstanceOf(MovedListItemChange.class, change1);
        assertSame(item1, moveChange1.getOldItem());
        assertSame(item1, moveChange1.getNewItem());
        assertEquals(0, moveChange1.getOldPosition());
        assertEquals(2, moveChange1.getNewPosition());

        final var change2 = changes.get(1);
        assertEquals(ListItemChangeAction.MOVED, change2.getAction());
        final var moveChange2 = assertInstanceOf(MovedListItemChange.class, change2);
        assertSame(item2, moveChange2.getOldItem());
        assertSame(item2, moveChange2.getNewItem());
        assertEquals(0, moveChange2.getOldPosition());
        assertEquals(1, moveChange2.getNewPosition());
    }

    @Test
    void detectChanges_whenUpdate_returnsExpectedResult() {
        final var item1 = new Item(1, "item1");
        final var item2 = new Item(2, "item2");
        final var item2b = new Item(2, "item2b");

        final var oldList = List.of(item1, item2);
        final var newList = List.of(item1, item2b);

        final var changes = detector.detectChanges(newList, oldList);

        assertEquals(1, changes.size());

        final var change = changes.get(0);
        assertEquals(ListItemChangeAction.UPDATED, change.getAction());
        final var updateChange = assertInstanceOf(UpdatedListItemChange.class, change);
        assertSame(item2, updateChange.getOldItem());
        assertSame(item2b, updateChange.getNewItem());
        assertEquals(1, updateChange.getPosition());
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

        final var changes = detector.detectChanges(newList, oldList);

        assertEquals(5, changes.size());

        final var change1 = changes.get(0);
        assertEquals(ListItemChangeAction.REMOVED, change1.getAction());
        final var removedChange = assertInstanceOf(RemovedListItemChange.class, change1);
        assertSame(item4, removedChange.getOldItem());
        assertEquals(3, removedChange.getOldPosition());

        final var change2 = changes.get(1);
        assertEquals(ListItemChangeAction.INSERTED, change2.getAction());
        final var insertedChange = assertInstanceOf(InsertedListItemChange.class, change2);
        assertSame(item5, insertedChange.getNewItem());
        assertEquals(3, insertedChange.getNewPosition());

        final var change3 = changes.get(2);
        assertEquals(ListItemChangeAction.MOVED, change3.getAction());
        final var movedChange1 = assertInstanceOf(MovedListItemChange.class, change3);
        assertSame(item1, movedChange1.getOldItem());
        assertSame(item1, movedChange1.getNewItem());
        assertEquals(0, movedChange1.getOldPosition());
        assertEquals(2, movedChange1.getNewPosition());

        final var change4 = changes.get(3);
        assertEquals(ListItemChangeAction.MOVED, change4.getAction());
        final var movedChange2 = assertInstanceOf(MovedListItemChange.class, change4);
        assertSame(item2, movedChange2.getOldItem());
        assertSame(item2b, movedChange2.getNewItem());
        assertEquals(0, movedChange2.getOldPosition());
        assertEquals(1, movedChange2.getNewPosition());

        final var change5 = changes.get(4);
        assertEquals(ListItemChangeAction.UPDATED, change5.getAction());
        final var updatedChange = assertInstanceOf(UpdatedListItemChange.class, change5);
        assertSame(item2, updatedChange.getOldItem());
        assertSame(item2b, updatedChange.getNewItem());
        assertEquals(1, updatedChange.getPosition());
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