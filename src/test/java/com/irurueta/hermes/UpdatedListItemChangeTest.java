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

class UpdatedListItemChangeTest {

    @Test
    void constructor_whenNullOldItem_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new UpdatedListItemChange<>(null, new Object(),
                0));
    }

    @Test
    void constructor_whenNullNewItem_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new UpdatedListItemChange<>(new Object(), null,
                0));
    }

    @Test
    void getAction_returnsExpectedValue() {
        final var change = new UpdatedListItemChange<>(new Object(), new Object(), 0);

        assertEquals(ListItemChangeAction.UPDATED, change.getAction());
    }

    @Test
    void getOldItem_returnsExpectedValue() {
        final var item = new Object();
        final var change = new UpdatedListItemChange<>(item, new Object(), 0);

        assertSame(item, change.getOldItem());
    }

    @Test
    void getNewItem_returnsExpectedValue() {
        final var item = new Object();
        final var change = new UpdatedListItemChange<>(new Object(), item, 0);

        assertSame(item, change.getNewItem());
    }

    @Test
    void getPosition_returnsExpectedValue() {
        final var change = new UpdatedListItemChange<>(new Object(), new Object(), 1);

        assertEquals(1, change.getPosition());
    }
}