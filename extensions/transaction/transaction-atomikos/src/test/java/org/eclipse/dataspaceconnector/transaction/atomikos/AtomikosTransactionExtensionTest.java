/*
 *  Copyright (c) 2022 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.transaction.atomikos;

import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.LOGGING;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AtomikosTransactionExtensionTest {
    AtomikosTransactionExtension extension;

    @Test
    void verifyEndToEndCommitTransaction() {
        var context = mock(ServiceExtensionContext.class);
        when(context.getSetting(LOGGING, null)).thenReturn("false");
        when(context.getConnectorId()).thenReturn(randomUUID().toString());

        extension.initialize(context);
        extension.start();
        extension.shutdown();
    }

    @BeforeEach
    void setUp() {
        extension = new AtomikosTransactionExtension();
    }
}
