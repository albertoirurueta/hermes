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

/**
 * Class to represent a change in a collection item that has been removed.
 *
 * @param <T> type of items in the collection.
 */
public class RemovedCollectionItemChange<T> extends CollectionItemChange {

    /**
     * Item that has been removed.
     */
    private final T oldItem;

    /**
     * Constructor.
     *
     * @param oldItem item that has been removed.
     * @throws IllegalArgumentException if provided item is null.
     */
    public RemovedCollectionItemChange(final T oldItem) {
        super(CollectionItemChangeAction.REMOVED);
        if (oldItem == null) {
            throw new IllegalArgumentException();
        }

        this.oldItem = oldItem;
    }

    /**
     * Returns the item that has been removed.
     * @return the item that has been removed.
     */
    public T getOldItem() {
        return oldItem;
    }
}
