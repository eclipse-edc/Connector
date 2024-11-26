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

package org.eclipse.edc.boot.system.testextensions;

import org.eclipse.edc.boot.system.TestObject;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;

public class RequiredDependentExtension implements ServiceExtension {
    public TestObject getTestObject() {
        return testObject;
    }

    @Inject
    private TestObject testObject;

    @Setting(key = "foo.bar", required = false)
    private String fooBar;
}
