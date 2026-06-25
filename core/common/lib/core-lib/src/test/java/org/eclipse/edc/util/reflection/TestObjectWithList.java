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

import java.util.List;

public class TestObjectWithList extends TestObject {

    private final List<TestObject> nestedObjects;

    private final TestObject nestedObject;

    public TestObjectWithList(String description, int priority, List<TestObject> nestedObjects) {
        this(description, priority, nestedObjects, null);
    }

    public TestObjectWithList(String description, int priority, List<TestObject> nestedObjects, TestObject nestedObject) {
        super(description, priority);
        this.nestedObjects = nestedObjects;
        this.nestedObject = nestedObject;
    }

    public List<TestObject> getNestedObjects() {
        return nestedObjects;
    }

    public TestObject getNestedObject() {
        return nestedObject;
    }
}
