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

package org.eclipse.dataspaceconnector.common.reflection;

import java.util.List;

public class TestObjectWithList extends TestObject {

    private final List<TestObject> nestedObjects;

    public TestObjectWithList(String description, int priority, List<TestObject> nestedObjects) {
        super(description, priority);
        this.nestedObjects = nestedObjects;
    }

    public List<TestObject> getNestedObjects() {
        return nestedObjects;
    }
}
