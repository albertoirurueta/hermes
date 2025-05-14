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
 * Contains information about the action that has been performed on a collection item.
 */
public enum CollectionItemChangeAction {
    /**
     * Indicates that an item has been inserted into a collection.
     */
    INSERTED,

    /**
     * Indicates that an item has been removed from a collection.
     */
    REMOVED,

    /**
     * Indicates that an item content has been updated in a collection.
     */
    UPDATED
}
