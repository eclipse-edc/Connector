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

package org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation;

import org.eclipse.edc.boot.system.injection.EdcInjectionException;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.SqlContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.BaseSqlDialectStatements;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class SqlContractNegotiationStoreExtensionTest {

    private SqlContractNegotiationStoreExtension extension;

    @Test
    void initialize(ServiceExtensionContext context, ObjectFactory factory) {
        var config = mock(Config.class);
        var provider = mock(SqlLeaseContextBuilderProvider.class);

        when(context.getConfig()).thenReturn(config);
        when(provider.createContextBuilder(any())).thenReturn(mock());

        context.registerService(DataSourceRegistry.class, mock(DataSourceRegistry.class));
        context.registerService(TransactionContext.class, mock(TransactionContext.class));
        context.registerService(ContractNegotiationStatements.class, null);
        context.registerService(TypeManager.class, new JacksonTypeManager());
        context.registerService(SqlLeaseContextBuilderProvider.class, provider);

        extension = factory.constructInstance(SqlContractNegotiationStoreExtension.class);

        extension.initialize(context);

        var service = context.getService(ContractNegotiationStore.class);
        assertThat(service).isInstanceOf(SqlContractNegotiationStore.class);
        assertThat(service).extracting("statements").isInstanceOf(BaseSqlDialectStatements.class);

    }

    @Test
    void initialize_withCustomSqlDialect(ServiceExtensionContext context, ObjectFactory factory) {
        var provider = mock(SqlLeaseContextBuilderProvider.class);
        when(provider.createContextBuilder(any())).thenReturn(mock());

        context.registerService(DataSourceRegistry.class, mock(DataSourceRegistry.class));
        context.registerService(TransactionContext.class, mock(TransactionContext.class));
        context.registerService(TypeManager.class, new JacksonTypeManager());
        var customSqlDialect = mock(ContractNegotiationStatements.class);
        context.registerService(ContractNegotiationStatements.class, customSqlDialect);
        context.registerService(SqlLeaseContextBuilderProvider.class, provider);

        extension = factory.constructInstance(SqlContractNegotiationStoreExtension.class);

        extension.initialize(context);

        var service = context.getService(ContractNegotiationStore.class);
        assertThat(service).isInstanceOf(SqlContractNegotiationStore.class);
        assertThat(service).extracting("statements").isSameAs(customSqlDialect);
    }

    @Test
    void initialize_missingDataSourceRegistry(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(DataSourceRegistry.class, null);
        context.registerService(TransactionContext.class, mock(TransactionContext.class));

        assertThatThrownBy(() -> factory.constructInstance(SqlContractNegotiationStoreExtension.class))
                .isInstanceOf(EdcInjectionException.class);
    }

}
