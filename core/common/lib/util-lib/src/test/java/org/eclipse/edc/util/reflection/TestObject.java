/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.util.reflection;

import java.util.ArrayList;
import java.util.List;

public class TestObject {
    private final String description;
    private final int priority;

    private final List<AnotherObject> listField = new ArrayList<>();

    private final AnotherObject embedded;


    public TestObject(String description, int priority) {
        this(description, priority, null);
    }

    public TestObject(String description, int priority, AnotherObject embedded) {
        this.description = description;
        this.priority = priority;
        this.embedded = embedded;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public List<AnotherObject> getListField() {
        return listField;
    }

    public AnotherObject getEmbedded() {
        return embedded;
    }
}
