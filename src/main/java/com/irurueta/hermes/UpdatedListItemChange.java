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
 * Class to represent a change in a collection item that has been updated.
 *
 * @param <T> type of items in the collection.
 */
public class UpdatedListItemChange<T> extends ListItemChange {

    /**
     * Item before being updated.
     */
    private final T oldItem;

    /**
     * Item after being updated.
     */
    private final T newItem;

    /**
     * Indicates the position where the item has been updated.
     */
    private final int position;

    /**
     * Constructor.
     *
     * @param oldItem item that before being updated.
     * @param newItem item after being updated.
     * @param position position where the item has been updated.
     * @throws IllegalArgumentException if either the old or new item is null.
     */
    public UpdatedListItemChange(final T oldItem, final T newItem, final int position) {
        super(ListItemChangeAction.UPDATED);
        if (oldItem == null || newItem == null) {
            throw new IllegalArgumentException();
        }

        this.oldItem = oldItem;
        this.newItem = newItem;
        this.position = position;
    }

    /**
     * Returns the item that before being updated.
     * @return the item that before being updated.
     */
    public T getOldItem() {
        return oldItem;
    }

    /**
     * Returns the item after being updated.
     * @return the item after being updated.
     */
    public T getNewItem() {
        return newItem;
    }

    /**
     * Returns the position where the item has been updated.
     * @return the position where the item has been updated.
     */
    public int getPosition() {
        return position;
    }
}
