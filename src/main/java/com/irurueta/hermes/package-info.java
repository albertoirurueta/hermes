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

/**
 * Package containing classes to compare and find changes between two sets of data.
 * <p>
 * To use this library, you must take into account:
 * <ul>
 *  <li>The collections being compared are position-aware or not (list vs. sets, maps, etc.)</li>
 *  <li>The items in your collection implement the ComparableItem interface or not</li>
 *  <li>For position-aware collections you want changes position values considering that changes are applied
 *  sequentially or not</li>
 * </ul>
 *
 * Based on those three factors, the following classes should be used:
 * <ul>
 *  <li>CollectionItemChangeDetector: when comparing non-position-aware collections and collection items do NOT
 *  implement any specific interface. (proper comparators must be provided).</li>
 *  <li>ComparableCollectionItemChangeDetector: when comparing non-position-aware collections and collection items
 *  implement ComparableItem interface.</li>
 *  <li>ListItemChangeDetector: when comparing position-aware collections and collection items do NOT implement any
 *  specific interface. (proper comparators must be provided).</li>
 *  <li>ComparableListItemChangeDetector: when comparing position-aware collections and collection items, implement
 *  ComparableItem interface.</li>
 *  <li>SequentialListItemChangeDetector: when comparing position-aware collections, collection items do NOT implement
 *  any specific interface, and returned changes (and their positions) are assumed to be applied sequentially.</li>
 *  <li>SequentialComparableListItemChangeDetector: when comparing position-aware collections, collection items
 *  implement ComparableItem interface, and returned changes (and their positions) are assumed to be applied
 *  sequentially.</li>
 * </ul>
 */
package com.irurueta.hermes;