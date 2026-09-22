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

import org.eclipse.edc.connector.controlplane.store.sql.partner.schema.postgres.PartnerGroupMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;
import static org.eclipse.edc.spi.query.Criterion.criterion;

public class PartnerGroupBaseSqlDialectStatements implements PartnerGroupStoreStatements {

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getParticipantContextIdColumn())
                .column(getIdColumn())
                .column(getNameColumn())
                .column(getDescriptionColumn())
                .jsonColumn(getPropertiesColumn())
                .column(getCreatedAtColumn())
                .insertInto(getPartnerGroupTable());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getNameColumn())
                .column(getDescriptionColumn())
                .jsonColumn(getPropertiesColumn())
                .update(getPartnerGroupTable(), criterion(getParticipantContextIdColumn(), "=", "?"), criterion(getIdColumn(), "=", "?"));
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement()
                .delete(getPartnerGroupTable(), criterion(getParticipantContextIdColumn(), "=", "?"), criterion(getIdColumn(), "=", "?"));
    }

    @Override
    public String getFindByIdTemplate() {
        return format("%s WHERE %s = ? AND %s = ?", getSelectStatement(), getParticipantContextIdColumn(), getIdColumn());
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getPartnerGroupTable());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectStatement(), querySpec, new PartnerGroupMapping(this), new PostgresqlOperatorTranslator());
    }
}
