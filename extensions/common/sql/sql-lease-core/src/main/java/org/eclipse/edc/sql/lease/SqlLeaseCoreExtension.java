/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.sql.lease;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

@Extension(value = SqlLeaseCoreExtension.NAME)
public class SqlLeaseCoreExtension implements ServiceExtension {

    public static final String NAME = "SQL Lease Core";
    @Inject
    private TransactionContext transactionContext;

    @Inject
    private Clock clock;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject(required = false)
    private LeaseStatements statements;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public SqlLeaseContextBuilderProvider leaseContextBuilderProvider(ServiceExtensionContext context) {
        return new SqlLeaseContextBuilderProviderImpl(transactionContext, getStatementImpl(), context.getRuntimeId(), clock, queryExecutor);
    }
    
    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private LeaseStatements getStatementImpl() {
        return statements != null ? statements : new BaseSqlLeaseStatements();
    }
}
