/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.transaction.local;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

/**
 * Support for transaction context backed by one or more local resources, including a {@link DataSourceRegistry}.
 */
@Provides({ DataSourceRegistry.class, TransactionContext.class })
@Extension(value = LocalTransactionExtension.NAME)
public class LocalTransactionExtension implements ServiceExtension {

    public static final String NAME = "Local Transaction";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var transactionContext = new LocalTransactionContext(context.getMonitor());
        var registry = new LocalDataSourceRegistry(transactionContext);

        context.registerService(TransactionContext.class, transactionContext);
        context.registerService(DataSourceRegistry.class, registry);
    }
}
