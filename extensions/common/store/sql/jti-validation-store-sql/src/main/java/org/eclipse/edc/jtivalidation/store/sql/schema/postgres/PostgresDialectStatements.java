/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.jtivalidation.store.sql.schema.postgres;

import org.eclipse.edc.jtivalidation.store.sql.schema.BaseSqlDialectStatements;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;

/**
 * Contains Postgres-specific SQL statements
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {

    public PostgresDialectStatements() {
        super(new PostgresqlOperatorTranslator());
    }

    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }

}
