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

import java.util.List;
import java.util.stream.IntStream;

/**
 * Class to detect changes between two lists of items.
 * Notice that returned changes in this class indicate the positions of the items with respect to the old or the new
 * list (regardless of the order in which the change is applied).
 *
 * @param <T> type of items.
 */
public class ListItemChangeDetector<T> extends AbstractListItemChangeDetector<T> {

    /**
     * Comparator to determine whether two items are equal (e.g., by their id).
     */
    private final ItemComparator<T> itemComparator;

    /**
     * Comparator to determine whether the content of two items is equal.
     */
    private final ItemContentComparator<T> itemContentComparator;

    /**
     * Constructor.
     *
     * @param itemComparator comparator to determine whether two items are equal.
     * @param itemContentComparator comparator to determine whether the content of two items is equal.
     * @throws IllegalArgumentException if any of the parameters is null.
     */
    public ListItemChangeDetector(final ItemComparator<T> itemComparator,
                                  final ItemContentComparator<T> itemContentComparator) {

        if (itemComparator == null || itemContentComparator == null) {
            throw new IllegalArgumentException();
        }

        this.itemComparator = itemComparator;
        this.itemContentComparator = itemContentComparator;
    }

    /**
     * Determines whether the content of an item has changed.
     *
     * @param newItem new item.
     * @param oldItem old item.
     * @return true if the content of the item has changed, false otherwise.
     */
    @Override
    protected boolean hasContentChanged(final T newItem, final T oldItem) {
        return !itemContentComparator.equalContent(newItem, oldItem);
    }

    /**
     * Determines whether an item is not contained in a list.
     *
     * @param items list of items.
     * @param item item to be checked.
     * @return true if the item is not contained in the list, false otherwise.
     */
    @Override
    protected boolean notContains(final List<T> items, final T item) {
        return items.stream().noneMatch(newItem -> itemComparator.equals(newItem, item));
    }

    /**
     * Determines the index of an item in a list.
     *
     * @param items list of items.
     * @param item item to be checked.
     * @return index of the item in the list, or -1 if the item is not contained in the list.
     */
    @Override
    protected int indexOf(final List<T> items, final T item) {
        return IntStream.range(0, items.size()).filter(index -> {
            final var otherItem = items.get(index);
            return itemComparator.equals(item, otherItem);
        }).findFirst().orElse(-1);
    }
}
