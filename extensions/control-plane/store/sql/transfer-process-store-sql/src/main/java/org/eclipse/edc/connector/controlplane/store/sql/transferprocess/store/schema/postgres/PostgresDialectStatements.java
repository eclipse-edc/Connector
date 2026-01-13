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

package org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.postgres;

import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.BaseSqlDialectStatements;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;

import java.time.Clock;

/**
 * Postgres-specific variations of the SQL statements based on Postgres's ability to use JSON operators and -functions.
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {

    public PostgresDialectStatements(LeaseStatements leaseStatements, Clock clock) {
        super(new PostgresqlOperatorTranslator(), leaseStatements, clock);
    }

    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }

}
