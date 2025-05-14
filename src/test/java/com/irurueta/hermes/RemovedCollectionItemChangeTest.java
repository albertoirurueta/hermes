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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RemovedCollectionItemChangeTest {

    @Test
    void constructor_whenNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new RemovedCollectionItemChange<>(null));
    }

    @Test
    void getOldItem_returnsExpectedValue() {
        final var item = new Object();
        final var change = new RemovedCollectionItemChange<>(item);

        assertSame(item, change.getOldItem());
    }

    @Test
    void getAction_returnsExpectedValue() {
        final var change = new RemovedCollectionItemChange<>(new Object());

        assertEquals(CollectionItemChangeAction.REMOVED, change.getAction());
    }
}