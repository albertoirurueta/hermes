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
 * Comparator to determine whether two items have equal content.
 *
 * @param <T> type of items.
 */
public interface ItemContentComparator<T> {

    /**
     * Determines whether the content of two items is equal.
     * @param item1 first item.
     * @param item2 second item.
     * @return true if the content of the items is equal, false otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean equalContent(T item1, T item2);
}
