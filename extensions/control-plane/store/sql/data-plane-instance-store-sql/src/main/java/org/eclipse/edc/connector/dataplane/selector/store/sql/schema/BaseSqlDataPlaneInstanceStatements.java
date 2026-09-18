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

package org.eclipse.edc.connector.dataplane.selector.store.sql.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class BaseSqlDataPlaneInstanceStatements implements DataPlaneInstanceStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlDataPlaneInstanceStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getDataPlaneInstanceTable(), getIdColumn());
    }

    @Override
    public String getAllTemplate() {
        return format("SELECT * FROM %s", getDataPlaneInstanceTable());
    }

    @Override
    public String getUpsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getStateColumn())
                .column(getStateTimestampColumn())
                .column(getCreatedAtColumn())
                .column(getUpdatedAtColumn())
                .column(getUrlColumn())
                .column(getLastActiveColumn())
                .column(getParticipantContextIdColumn())
                .jsonColumn(getAllowedSourceTypesColumn())
                .jsonColumn(getAllowedTransferTypesColumn())
                .jsonColumn(getDestinationProvisionTypesColumn())
                .jsonColumn(getLabelsColumn())
                .jsonColumn(getPropertiesColumn())
                .jsonColumn(getAuthorizationProfileColumn())
                .upsertInto(getDataPlaneInstanceTable(), getIdColumn());
    }

    @Override
    public String getSelectTemplate() {
        return "SELECT * FROM %s".formatted(getDataPlaneInstanceTable());
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getDataPlaneInstanceTable(), getIdColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new DataPlaneInstanceMapping(this), operatorTranslator);
    }
}
