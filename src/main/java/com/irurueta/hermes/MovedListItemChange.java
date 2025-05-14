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
 * Class to represent a change in a list item that has been moved to a different position.
 *
 * @param <T> type of items in the list.
 */
public class MovedListItemChange<T> extends ListItemChange {

    /**
     * Item that before being updated.
     */
    private final T oldItem;

    /**
     * Item after being updated.
     */
    private final T newItem;

    /**
     * This value indicates the position of the item before it was moved.
     */
    private final int oldPosition;

    /**
     * This value indicates the position of the item after being moved.
     */
    private final int newPosition;

    /**
     * Constructor.
     *
     * @param oldItem item that has been moved before being moved.
     * @param newItem item that has been moved after being moved.
     * @param oldPosition position of the item before it was moved.
     * @param newPosition position of the item after being moved.
     * @throws IllegalArgumentException if either the old or new item is null.
     */
    public MovedListItemChange(final T oldItem, final T newItem, final int oldPosition, final int newPosition) {
        super(ListItemChangeAction.MOVED);
        if (oldItem == null || newItem == null) {
            throw new IllegalArgumentException();
        }

        this.oldItem = oldItem;
        this.newItem = newItem;
        this.oldPosition = oldPosition;
        this.newPosition = newPosition;
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
     * Returns the start position of the item before it was moved.
     * @return the start position of the item before it was moved.
     */
    public int getOldPosition() {
        return oldPosition;
    }

    /**
     * Returns the end position of the item after being moved.
     * @return the end position of the item after being moved.
     */
    public int getNewPosition() {
        return newPosition;
    }
}
