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
 * Class to represent a change in a list item that has been added.
 *
 * @param <T> type of items in the list.
 */
public class InsertedListItemChange<T> extends ListItemChange {

    /**
     * Item that has been added.
     */
    private final T newItem;

    /**
     * Indicates the position where the item has been added on the new list.
     */
    private final int newPosition;

    /**
     * Constructor.
     *
     * @param newItem item that has been added.
     * @param newPosition position where the item has been added, removed or updated.
     * @throws IllegalArgumentException if provided item is null.
     */
    public InsertedListItemChange(final T newItem, final int newPosition) {
        super(ListItemChangeAction.INSERTED);
        if (newItem == null) {
            throw new IllegalArgumentException();
        }

        this.newItem = newItem;
        this.newPosition = newPosition;
    }

    /**
     * Returns the item that has been added.
     * @return the item that has been added.
     */
    public T getNewItem() {
        return newItem;
    }

    /**
     * Returns the position where the item has been added.
     * @return the position where the item has been added.
     */
    public int getNewPosition() {
        return newPosition;
    }
}
