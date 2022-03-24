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

package org.eclipse.dataspaceconnector.dataplane.selector;

import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;
import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstanceImpl;

public class TestFunctions {

    public static DataPlaneInstance createInstance(String id) {
        return DataPlaneInstanceImpl.Builder.newInstance()
                .id(id)
                .url("http://somewhere.com:1234/api/v1")
                .build();
    }

    public static DataPlaneInstanceImpl.Builder createInstanceBuilder(String id) {
        return DataPlaneInstanceImpl.Builder.newInstance()
                .id(id)
                .url("http://somewhere.com:1234/api/v1");
    }
}
