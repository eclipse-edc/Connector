/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.iam.decentralizedclaims.store.sql.schema.postgres;

import org.eclipse.edc.iam.decentralizedclaims.store.sql.schema.BaseSqlDcpScopeStatements;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;

public class PostgresDcpScopeStatements extends BaseSqlDcpScopeStatements {

    public PostgresDcpScopeStatements() {
        super(new PostgresqlOperatorTranslator());
    }

    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }
}
