/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Tests
 *
 */

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;

import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(EdcExtension.class)
public class AbstractSqlContractDefinitionStoreTest {
    private final AtomicReference<ServiceExtensionContext> contextRef = new AtomicReference<>();

    @BeforeEach
    void before(EdcExtension extension) {
        extension.registerSystemExtension(ServiceExtension.class, new ServiceExtension() {
            public void initialize(ServiceExtensionContext context) {
                contextRef.set(context);
            }
        });
    }

    @AfterEach
    void afterTestExecution() {
        contextRef.set(null);
    }

    protected DataSourceRegistry getDataSourceRegistry() {
        return contextRef.get().getService(DataSourceRegistry.class);
    }

    protected TransactionContext getTransactionContext() {
        return contextRef.get().getService(TransactionContext.class);
    }

    protected ContractDefinitionStore getContractDefinitionStore() {
        return contextRef.get().getService(ContractDefinitionStore.class);
    }
}
