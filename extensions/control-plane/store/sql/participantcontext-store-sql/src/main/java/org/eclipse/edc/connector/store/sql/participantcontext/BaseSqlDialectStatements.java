/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.store.sql.participantcontext;

import org.eclipse.edc.connector.store.sql.participantcontext.schema.postgres.ParticipantContextMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements ParticipantContextStoreStatements {
    @Override
    public String getInsertTemplate() {
        return executeStatement()
                       .column(getIdColumn())
                       .column(getCreateTimestampColumn())
                       .column(getLastModifiedTimestampColumn())
                       .column(getStateColumn())
                       .jsonColumn(getPropertiesColumn())
                       .insertInto(getParticipantContextTable());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                       .column(getIdColumn())
                       .column(getCreateTimestampColumn())
                       .column(getLastModifiedTimestampColumn())
                       .column(getStateColumn())
                       .jsonColumn(getPropertiesColumn())
                       .update(getParticipantContextTable(), getIdColumn());
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getParticipantContextTable(), getIdColumn());
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getParticipantContextTable(), getIdColumn());

    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = getSelectStatement();
        return new SqlQueryStatement(select, querySpec, new ParticipantContextMapping(this), new PostgresqlOperatorTranslator());
    }

    @Override
    public String getSelectStatement() {
        return format("SELECT * FROM %s", getParticipantContextTable());
    }
}
