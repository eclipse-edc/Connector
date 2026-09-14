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

package org.eclipse.edc.connector.dataplane.selector.store.sql.schema.postgres;

import org.eclipse.edc.connector.dataplane.selector.store.sql.schema.BaseSqlDataPlaneInstanceStatements;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;

import java.time.Clock;

public class PostgresDataPlaneInstanceStatements extends BaseSqlDataPlaneInstanceStatements {

    public PostgresDataPlaneInstanceStatements(Clock clock) {
        super(new PostgresqlOperatorTranslator(), clock);
    }

    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }
}
