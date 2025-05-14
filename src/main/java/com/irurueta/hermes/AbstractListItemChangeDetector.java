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
import java.util.List;

/**
 * Abstract class to detect changes between two lists of items.
 * Notice that returned changes in this class indicate the positions of the items with respect to the old or the new
 * list (regardless of the order in which the change is applied).
 *
 * @param <T> type of items.
 */
public abstract class AbstractListItemChangeDetector<T> {

    /**
     * Detects changes between two lists of items.
     *
     * @param newItems new items.
     * @param oldItems old items.
     * @return a list of found changes.
     */
    public List<ListItemChange> detectChanges(final List<T> newItems, final List<T> oldItems) {
        if (oldItems == null || newItems == null) {
            throw new IllegalArgumentException();
        }

        final var changes = new ArrayList<ListItemChange>();

        final var newCopy = new ArrayList<>(newItems);
        final var oldCopy = new ArrayList<>(oldItems);

        // removes
        changes.addAll(buildRemoves(newCopy, oldCopy));

        // inserts
        changes.addAll(buildInserts(newCopy, oldCopy));

        // moves
        changes.addAll(buildMoves(newCopy, oldCopy));

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
     * Determines whether an item is not contained in a list.
     *
     * @param items list of items.
     * @param item item to be checked.
     * @return true if the item is not contained in the list, false otherwise.
     */
    protected abstract boolean notContains(final List<T> items, final T item);

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
     *
     * @param newItemsCopy new items copy.
     * @param oldItemsCopy old items copy.
     * @return a list of detected remove changes.
     */
    private List<RemovedListItemChange<T>> buildRemoves(final List<T> newItemsCopy, final List<T> oldItemsCopy) {
        final var removes = new ArrayList<RemovedListItemChange<T>>();

        for (var i = 0; i < oldItemsCopy.size(); i++) {
            final var oldItem = oldItemsCopy.get(i);
            if (notContains(newItemsCopy, oldItem)) {
                // this item has been removed from the new list
                removes.add(new RemovedListItemChange<>(oldItem, i));
            }
        }
        return removes;
    }

    /**
     * Finds inserted items and build a list of detected insert changes.
     *
     * @param newItemsCopy new items copy.
     * @param oldItemsCopy old items copy.
     * @return a list of detected insert changes.
     */
    private List<InsertedListItemChange<T>> buildInserts(final List<T> newItemsCopy, final List<T> oldItemsCopy) {
        final var inserts = new ArrayList<InsertedListItemChange<T>>();

        final var newSize = newItemsCopy.size();
        for (var i = 0; i < newSize; i++) {
            final var newItem = newItemsCopy.get(i);
            if (notContains(oldItemsCopy, newItem)) {
                // this item has been added on the new list
                inserts.add(new InsertedListItemChange<>(newItem, i));
            }
        }

        return inserts;
    }

    /**
     * Finds moved items and build a list of detected move changes.
     *
     * @param newItemsCopy new items copy.
     * @param oldItemsCopy old items copy.
     * @return a list of detected move changes.
     */
    private List<MovedListItemChange<T>> buildMoves(final List<T> newItemsCopy, final List<T> oldItemsCopy) {
        final var moves = new ArrayList<MovedListItemChange<T>>();

        for (var i = 0; i < oldItemsCopy.size(); i++) {
            final var item = oldItemsCopy.get(i);

            final var pos2 = indexOf(newItemsCopy, item);

            if (pos2 >= 0 && i != pos2) {
                // item has been moved
                final var oldItem = oldItemsCopy.get(i);
                final var newItem = newItemsCopy.get(pos2);
                moves.add(new MovedListItemChange<>(oldItem, newItem, i, pos2));
            }
        }

        return moves;
    }

    /**
     * Finds changed items and build a list of detected update changes.
     *
     * @param newItemsCopy new items copy.
     * @param oldItemsCopy old items copy.
     * @return a list of detected update changes.
     */
    private List<UpdatedListItemChange<T>> buildChanges(final List<T> newItemsCopy, final List<T> oldItemsCopy) {
        final var changes = new ArrayList<UpdatedListItemChange<T>>();

        var pos = 0;
        for (final var newItem : newItemsCopy) {
            final var oldPos = indexOf(oldItemsCopy, newItem);
            if (oldPos >= 0) {
                final var oldItem = oldItemsCopy.get(oldPos);

                // compare both items
                if (hasContentChanged(newItem, oldItem)) {
                    // item has changed
                    changes.add(new UpdatedListItemChange<>(oldItem, newItem, pos));
                }
            }
            pos++;
        }

        return changes;
    }
}
