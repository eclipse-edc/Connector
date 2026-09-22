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

package org.eclipse.edc.connector.controlplane.store.sql.partner.schema.postgres;

import org.eclipse.edc.connector.controlplane.store.sql.partner.PartnerBaseSqlDialectStatements;
import org.eclipse.edc.sql.dialect.PostgresDialect;

public class PartnerPostgresDialectStatements extends PartnerBaseSqlDialectStatements {

    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }
}
