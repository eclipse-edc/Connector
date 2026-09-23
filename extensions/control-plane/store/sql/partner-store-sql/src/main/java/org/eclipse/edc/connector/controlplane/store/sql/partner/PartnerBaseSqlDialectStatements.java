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

package org.eclipse.edc.connector.controlplane.store.sql.partner;

import org.eclipse.edc.connector.controlplane.store.sql.partner.schema.postgres.PartnerMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;
import static org.eclipse.edc.spi.query.Criterion.criterion;

public class PartnerBaseSqlDialectStatements implements PartnerStoreStatements {

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getParticipantContextIdColumn())
                .column(getIdColumn())
                .column(getIdentityColumn())
                .column(getNameColumn())
                .jsonColumn(getPropertiesColumn())
                .jsonColumn(getGroupIdsColumn())
                .column(getCreatedAtColumn())
                .insertInto(getPartnerTable());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getIdentityColumn())
                .column(getNameColumn())
                .jsonColumn(getPropertiesColumn())
                .jsonColumn(getGroupIdsColumn())
                .update(getPartnerTable(), criterion(getParticipantContextIdColumn(), "=", "?"), criterion(getIdColumn(), "=", "?"));
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement()
                .delete(getPartnerTable(), criterion(getParticipantContextIdColumn(), "=", "?"), criterion(getIdColumn(), "=", "?"));
    }

    @Override
    public String getFindByIdTemplate() {
        return format("%s WHERE %s = ? AND %s = ?", getSelectStatement(), getParticipantContextIdColumn(), getIdColumn());
    }

    @Override
    public String getFindByIdentityTemplate() {
        return format("%s WHERE %s = ? AND %s = ?", getSelectStatement(), getParticipantContextIdColumn(), getIdentityColumn());
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getPartnerTable());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectStatement(), querySpec, new PartnerMapping(this), new PostgresqlOperatorTranslator());
    }
}
