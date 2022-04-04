/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.core.version;

import org.eclipse.dataspaceconnector.ids.spi.version.ConnectorVersionProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConnectorVersionProviderImplTest {

    private ConnectorVersionProvider connectorVersionProvider;

    @BeforeEach
    void setUp() {
        connectorVersionProvider = new ConnectorVersionProviderImpl();
    }

    @Test
    void testDoesNotThrow() {
        Assertions.assertDoesNotThrow(() -> {
            connectorVersionProvider.getVersion();
        });
    }
}
