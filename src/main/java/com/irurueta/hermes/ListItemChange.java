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
 * Contains information about found changes in a list item.
 */
public abstract class ListItemChange {

    /**
     * Action that has been performed on the item, such as being added, removed, changed or moved within a list.
     */
    private final ListItemChangeAction action;

    /**
     * Constructor.
     *
     * @param listItemChangeAction action that has been performed on the item, such as being added, removed, changed or
     *                             moved within a list.
     */
    public ListItemChange(final ListItemChangeAction listItemChangeAction) {
        this.action = listItemChangeAction;
    }

    /**
     * Gets the action that has been performed on the item, such as being added, removed, changed or moved within a
     * collection.
     * @return the action that has been performed on the item.
     */
    public ListItemChangeAction getAction() {
        return action;
    }
}
