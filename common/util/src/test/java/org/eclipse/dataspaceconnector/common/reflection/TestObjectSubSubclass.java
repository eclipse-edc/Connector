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

public class TestObjectSubSubclass extends TestObjectSubclass {
    private final String description;

    public TestObjectSubSubclass(String description, int priority, String testProperty) {
        super(description, priority, testProperty);
        this.description = "Sub_" + description;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
