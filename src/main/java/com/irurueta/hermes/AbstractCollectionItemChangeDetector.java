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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Abstract class to detect changes in two collections of items.
 *
 * @param <T> type of items.
 */
public abstract class AbstractCollectionItemChangeDetector<T> {

    /**
     * Detects changes between two collections of items.
     *
     * @param newItems new items.
     * @param oldItems old items.
     * @return a list of found changes.
     */
    public Collection<CollectionItemChange> detectChanges(final Collection<T> newItems, final Collection<T> oldItems) {
        if (oldItems == null || newItems == null) {
            throw new IllegalArgumentException();
        }

        final var changes = new ArrayList<CollectionItemChange>();

        final var newCopy = new ArrayList<>(newItems);
        final var oldCopy = new ArrayList<>(oldItems);

        // removes
        changes.addAll(buildRemoves(newCopy, oldCopy));

        // inserts
        changes.addAll(buildInserts(newCopy, oldCopy));

        // changed items
        changes.addAll(buildChanges(newCopy, oldCopy));

        return changes;
    }

    /**
     * Determines whether the content of an item has changed.
     *
     * @param newItem new item.
     * @param oldItem old item.
     * @return true if the content of the item has changed, false otherwise.
     */
    protected abstract boolean hasContentChanged(final T newItem, final T oldItem);

    /**
     * Determines whether an item is not contained in a collection.
     *
     * @param items list of items.
     * @param item item to be checked.
     * @return true if the item is not contained in the list, false otherwise.
     */
    protected abstract boolean notContains(final Collection<T> items, final T item);

    /**
     * Determines the index of an item in a list.
     *
     * @param items list of items.
     * @param item item to be checked.
     * @return index of the item in the list, or -1 if the item is not contained in the list.
     */
    protected abstract int indexOf(final List<T> items, final T item);

    /**
     * Finds removed items and build a list of detected remove changes.
     * Notice that this method modifies the old items copy to avoid checking removed items again.
     *
     * @param newItemsCopy new items copy.
     * @param oldItemsCopy old items copy.
     * @return a list of detected remove changes.
     */
    private Collection<RemovedCollectionItemChange<T>> buildRemoves(
            final Collection<T> newItemsCopy, final Collection<T> oldItemsCopy) {

        final var removes = new ArrayList<RemovedCollectionItemChange<T>>();
        final var oldIterator = oldItemsCopy.iterator();

        while (oldIterator.hasNext()) {
            final var oldItem = oldIterator.next();
            if (notContains(newItemsCopy, oldItem)) {
                // item has been removed on the new list, we remove it to avoid checking it again
                oldIterator.remove();
                removes.add(new RemovedCollectionItemChange<>(oldItem));
            }
        }
        return removes;
    }

    /**
     * Finds inserted items and build a list of detected insert changes.
     * Notice that this method modifies the old items copy to avoid checking inserted items again.
     *
     * @param newItemsCopy new items copy.
     * @param oldItemsCopy old items copy.
     * @return a list of detected insert changes.
     */
    private Collection<InsertedCollectionItemChange<T>> buildInserts(
            final List<T> newItemsCopy, final List<T> oldItemsCopy) {

        final var inserts = new ArrayList<InsertedCollectionItemChange<T>>();
        final var newIterator = newItemsCopy.iterator();

        var i = 0;
        while (newIterator.hasNext()) {
            final var newItem = newIterator.next();
            if (notContains(oldItemsCopy, newItem)) {
                // this item has been added on the new list
                inserts.add(new InsertedCollectionItemChange<>(newItem));

                // make old items resemble new items so that changes and moves are later detected at proper positions
                if (i >= oldItemsCopy.size()) {
                    oldItemsCopy.add(newItem);
                } else {
                    oldItemsCopy.add(i, newItem);
                }
            }
            i++;
        }

        return inserts;
    }

    /**
     * Finds changed items and build a list of detected update changes.
     *
     * @param newItemsCopy new items copy.
     * @param oldItemsCopy old items copy.
     * @return a list of detected update changes.
     */
    private List<UpdatedCollectionItemChange<T>> buildChanges(
            final List<T> newItemsCopy, final List<T> oldItemsCopy) {

        final var changes = new ArrayList<UpdatedCollectionItemChange<T>>();

        for (final var newItem : newItemsCopy) {
            final var oldPos = indexOf(oldItemsCopy, newItem);
            if (oldPos >= 0) {
                final var oldItem = oldItemsCopy.get(oldPos);

                // compare both items
                if (hasContentChanged(newItem, oldItem)) {
                    // item has changed
                    changes.add(new UpdatedCollectionItemChange<>(oldItem, newItem));
                }
            }
        }

        return changes;
    }
}
