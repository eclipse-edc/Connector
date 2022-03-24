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

import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

public class TestFunctions {

    public static DataAddress createAddress(String type) {
        return DataAddress.Builder.newInstance()
                .type("test-type")
                .keyName(type)
                .property("someprop", "someval")
                .build();
    }

}
