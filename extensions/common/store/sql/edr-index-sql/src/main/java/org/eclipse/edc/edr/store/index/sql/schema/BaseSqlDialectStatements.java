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

package org.eclipse.edc.edr.store.index.sql.schema;

import org.eclipse.edc.edr.store.index.sql.schema.postgres.EndpointDataReferenceEntryMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements EndpointDataReferenceEntryStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getEdrEntryTable(), getTransferProcessIdColumn());
    }

    @Override
    public String getFindByTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getEdrEntryTable(), getTransferProcessIdColumn());
    }

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getTransferProcessIdColumn())
                .column(getAssetIdColumn())
                .column(getProviderIdColumn())
                .column(getAgreementIdColumn())
                .column(getContractNegotiationIdColumn())
                .column(getCreatedAtColumn())
                .column(getParticipantContextIdColumn())
                .insertInto(getEdrEntryTable());
    }

    @Override
    public String getCountTemplate() {
        return format("SELECT COUNT (%s) FROM %s WHERE %s = ?",
                getTransferProcessIdColumn(),
                getEdrEntryTable(),
                getTransferProcessIdColumn());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getTransferProcessIdColumn())
                .column(getAssetIdColumn())
                .column(getProviderIdColumn())
                .column(getAgreementIdColumn())
                .column(getContractNegotiationIdColumn())
                .column(getCreatedAtColumn())
                .update(getEdrEntryTable(), getTransferProcessIdColumn());

    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = format("SELECT * FROM %s", getEdrEntryTable());
        return new SqlQueryStatement(select, querySpec, new EndpointDataReferenceEntryMapping(this), operatorTranslator);
    }

}
