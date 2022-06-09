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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.dataspaceconnector.sql.contractnegotiation;

import org.eclipse.dataspaceconnector.junit.launcher.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.EdcInjectionException;
import org.eclipse.dataspaceconnector.spi.system.injection.ObjectFactory;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.sql.contractnegotiation.store.ContractNegotiationStatements;
import org.eclipse.dataspaceconnector.sql.contractnegotiation.store.PostgresStatements;
import org.eclipse.dataspaceconnector.sql.contractnegotiation.store.SqlContractNegotiationStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ExtendWith(DependencyInjectionExtension.class)
class SqlContractNegotiationStoreExtensionTest {

    private SqlContractNegotiationStoreExtension extension;

    @Test
    void initialize(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(DataSourceRegistry.class, mock(DataSourceRegistry.class));
        context.registerService(TransactionContext.class, mock(TransactionContext.class));

        extension = factory.constructInstance(SqlContractNegotiationStoreExtension.class);

        extension.initialize(context);

        var service = context.getService(ContractNegotiationStore.class);
        assertThat(service).isInstanceOf(SqlContractNegotiationStore.class);
        assertThat(service).extracting("statements").isInstanceOf(PostgresStatements.class);
    }

    @Test
    void initialize_withCustomSqlDialect(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(DataSourceRegistry.class, mock(DataSourceRegistry.class));
        context.registerService(TransactionContext.class, mock(TransactionContext.class));
        var customSqlDialect = mock(ContractNegotiationStatements.class);
        context.registerService(ContractNegotiationStatements.class, customSqlDialect);

        extension = factory.constructInstance(SqlContractNegotiationStoreExtension.class);

        extension.initialize(context);

        var service = context.getService(ContractNegotiationStore.class);
        assertThat(service).isInstanceOf(SqlContractNegotiationStore.class);
        assertThat(service).extracting("statements").isSameAs(customSqlDialect);
    }

    @Test
    void initialize_missingDataSourceRegistry(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(TransactionContext.class, mock(TransactionContext.class));

        assertThatThrownBy(() -> factory.constructInstance(SqlContractNegotiationStoreExtension.class))
                .isInstanceOf(EdcInjectionException.class);
    }

    @Test
    void initialize_missingTransactionContext(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(TransactionContext.class, mock(TransactionContext.class));

        assertThatThrownBy(() -> factory.constructInstance(SqlContractNegotiationStoreExtension.class))
                .isInstanceOf(EdcInjectionException.class);

    }

}