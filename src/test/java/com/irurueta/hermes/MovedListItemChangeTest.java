package com.irurueta.hermes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovedListItemChangeTest {

    @Test
    void constructor_whenNullOldItem_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new MovedListItemChange<>(null, new Object(), 0, 0));
    }

    @Test
    void constructor_whenNullNewItem_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new MovedListItemChange<>(new Object(), null, 0, 0));
    }

    @Test
    void getOldItem_returnsExpectedValue() {
        final var item = new Object();
        final var change = new MovedListItemChange<>(item, new Object(), 0, 0);

        assertSame(item, change.getOldItem());
    }

    @Test
    void getNewItem_returnsExpectedValue() {
        final var item = new Object();
        final var change = new MovedListItemChange<>(new Object(), item, 0, 0);

        assertSame(item, change.getNewItem());
    }

    @Test
    void getOldPosition_returnsExpectedValue() {
        final var change = new MovedListItemChange<>(new Object(), new Object(), 1, 0);

        assertEquals(1, change.getOldPosition());
    }

    @Test
    void getNewPosition_returnsExpectedValue() {
        final var change = new MovedListItemChange<>(new Object(), new Object(), 0, 1);

        assertEquals(1, change.getNewPosition());
    }

    @Test
    void getAction_returnsExpectedValue() {
        final var change = new MovedListItemChange<>(new Object(), new Object(), 0, 1);

        assertEquals(ListItemChangeAction.MOVED, change.getAction());
    }
}