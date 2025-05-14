package com.irurueta.hermes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InsertedListItemChangeTest {

    @Test
    void constructor_whenNullItem_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new InsertedListItemChange<>(null, 0));
    }

    @Test
    void getNewItem_returnsExpectedValue() {
        final var item = new Object();
        final var change = new InsertedListItemChange<>(item, 0);

        assertSame(item, change.getNewItem());
    }

    @Test
    void getNewPosition_returnsExpectedValue() {
        final var change = new InsertedListItemChange<>(new Object(), 1);

        assertEquals(1, change.getNewPosition());
    }

    @Test
    void getAction_returnsExpectedValue() {
        final var change = new InsertedListItemChange<>(new Object(), 1);
        assertEquals(ListItemChangeAction.INSERTED, change.getAction());
    }
}