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
 * Interface of items that can be compared to other items in a collection.
 * @param <T> type of item.
 */
public interface ComparableItem<T extends ComparableItem<T>> {

    /**
     * Determines whether this item is equal to another item (e.g., has the same identifier).
     * This method MUST NOT check if the content of all items is equal, only if they represent the same item.
     * @param other another item to be compared.
     * @return true if both items are equal, false otherwise.
     */
    boolean equals(Object other);

    /**
     * Determines whether the content of this item is equal to the content of another item.
     * @param other another item to be compared.
     * @return true if the content of both items is equal, false otherwise.
     */
    boolean equalContent(T other);
}
