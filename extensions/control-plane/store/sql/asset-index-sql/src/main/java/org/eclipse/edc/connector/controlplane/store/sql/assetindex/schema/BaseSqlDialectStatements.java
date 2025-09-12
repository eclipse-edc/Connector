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
 *       ZF Friedrichshafen AG - added private property support
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema;


import org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema.postgres.AssetMapping;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import java.util.List;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements AssetStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getInsertAssetTemplate() {
        return executeStatement()
                .column(getAssetIdColumn())
                .column(getCreatedAtColumn())
                .jsonColumn(getPropertiesColumn())
                .jsonColumn(getPrivatePropertiesColumn())
                .jsonColumn(getDataAddressColumn())
                .column(getParticipantContextIdColumn())
                .insertInto(getAssetTable());
    }

    @Override
    public String getUpdateAssetTemplate() {
        return executeStatement()
                .jsonColumn(getPropertiesColumn())
                .jsonColumn(getPrivatePropertiesColumn())
                .jsonColumn(getDataAddressColumn())
                .update(getAssetTable(), getAssetIdColumn());
    }

    @Override
    public String getCountAssetByIdClause() {
        return format("SELECT COUNT(*) AS %s FROM %s WHERE %s = ?",
                getCountVariableName(),
                getAssetTable(),
                getAssetIdColumn());
    }

    @Override
    public String getSelectAssetTemplate() {
        return format("SELECT * FROM %s AS a", getAssetTable());
    }

    @Override
    public String getDeleteAssetByIdTemplate() {
        return executeStatement()
                .delete(getAssetTable(), getAssetIdColumn());
    }

    @Override
    public String getCountVariableName() {
        return "COUNT";
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectAssetTemplate(), querySpec, new AssetMapping(this), operatorTranslator);
    }

    @Override
    public SqlQueryStatement createQuery(List<Criterion> criteria) {
        return createQuery(QuerySpec.Builder.newInstance()
                .filter(criteria)
                .offset(0)
                .limit(Integer.MAX_VALUE)
                .build());
    }

}
