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

import java.util.Collection;
import java.util.List;

/**
 * Class to detect changes between two collections of comparable items.
 *
 * @param <T> type of items (which must extend from {@link ComparableItem}).
 */
public class ComparableCollectionItemChangeDetector<T extends ComparableItem<T>>
        extends AbstractCollectionItemChangeDetector<T> {

    /**
     * Determines whether the content of an item has changed.
     *
     * @param newItem new item.
     * @param oldItem old item.
     * @return true if the content of the item has changed, false otherwise.
     */
    @Override
    protected boolean hasContentChanged(final T newItem, final T oldItem) {
        return !newItem.equalContent(oldItem);
    }

    /**
     * Determines whether an item is not contained in a collection.
     *
     * @param items list of items.
     * @param item item to be checked.
     * @return true if the item is not contained in the list, false otherwise.
     */
    @Override
    protected boolean notContains(final Collection<T> items, final T item) {
        return !items.contains(item);
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
        return items.indexOf(item);
    }
}
