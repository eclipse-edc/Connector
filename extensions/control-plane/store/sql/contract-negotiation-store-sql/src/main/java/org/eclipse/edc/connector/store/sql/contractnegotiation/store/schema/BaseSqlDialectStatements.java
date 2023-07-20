/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.store.sql.contractnegotiation.store.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.dialect.BaseSqlDialect;
import org.eclipse.edc.sql.statement.ColumnEntry;
import org.eclipse.edc.sql.statement.SqlExecuteStatement;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

/**
 * Provides statements required by the ContractNegotiationStore in generic SQL, that is not specific to a particular
 * database. This class is abstract, because there are some statements that cannot be expressed in a generic way.
 */
public class BaseSqlDialectStatements implements ContractNegotiationStatements {
    @Override
    public String getFindTemplate() {
        return format("SELECT * FROM %s LEFT OUTER JOIN %s ON %s.%s = %s.%s WHERE %s.%s = ?;", getContractNegotiationTable(), getContractAgreementTable(),
                getContractNegotiationTable(), getContractAgreementIdFkColumn(), getContractAgreementTable(), getContractAgreementIdColumn(), getContractNegotiationTable(), getIdColumn());
    }

    @Override
    public String getFindContractAgreementTemplate() {
        return format("SELECT * FROM %s where %s=?;", getContractAgreementTable(), getContractAgreementIdColumn());
    }

    @Override
    public String getUpdateNegotiationTemplate() {
        return SqlExecuteStatement.newInstance(getFormatJsonOperator())
                .column(getStateColumn())
                .column(getStateCountColumn())
                .column(getStateTimestampColumn())
                .column(getErrorDetailColumn())
                .jsonColumn(getContractOffersColumn())
                .jsonColumn(getCallbackAddressesColumn())
                .jsonColumn(getTraceContextColumn())
                .column(getContractAgreementIdFkColumn())
                .column(getUpdatedAtColumn())
                .column(getPendingColumn())
                .update(getContractNegotiationTable(), ColumnEntry.standardColumn(getIdColumn()));
    }

    @Override
    public String getInsertNegotiationTemplate() {
        return SqlExecuteStatement.newInstance(getFormatJsonOperator())
                .column(getIdColumn())
                .column(getCorrelationIdColumn())
                .column(getCounterPartyIdColumn())
                .column(getCounterPartyAddressColumn())
                .column(getTypeColumn())
                .column(getProtocolColumn())
                .column(getStateColumn())
                .column(getStateCountColumn())
                .column(getStateTimestampColumn())
                .column(getErrorDetailColumn())
                .column(getContractAgreementIdFkColumn())
                .jsonColumn(getContractOffersColumn())
                .jsonColumn(getCallbackAddressesColumn())
                .jsonColumn(getTraceContextColumn())
                .column(getCreatedAtColumn())
                .column(getUpdatedAtColumn())
                .column(getPendingColumn())
                .insertInto(getContractNegotiationTable());
    }

    @Override
    public String getDeleteTemplate() {
        return format("DELETE FROM %s WHERE %s = ? AND %s IS NULL;", getContractNegotiationTable(), getIdColumn(), getContractAgreementIdFkColumn());
    }

    @Override
    public String getSelectFromAgreementsTemplate() {
        // todo: add WHERE ... AND ... ORDER BY... statements here
        return format("SELECT * FROM %s", getContractAgreementTable());
    }

    @Override
    public String getInsertAgreementTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?%s);",
                getContractAgreementTable(), getContractAgreementIdColumn(), getProviderAgentColumn(), getConsumerAgentColumn(),
                getSigningDateColumn(), getAssetIdColumn(), getPolicyColumn(), getFormatJsonOperator());
    }

    @Override
    public String getUpdateAgreementTemplate() {
        return format("UPDATE %s SET %s=?, %s=?, %s=?, %s=?, %s=?%s WHERE %s =?",
                getContractAgreementTable(), getProviderAgentColumn(), getConsumerAgentColumn(), getSigningDateColumn(),
                getAssetIdColumn(), getPolicyColumn(), getFormatJsonOperator(), getContractAgreementIdColumn());
    }

    @Override
    public String getSelectNegotiationsTemplate() {
        return format("SELECT * FROM %s LEFT JOIN %s agr ON %s.%s = agr.%s", getContractNegotiationTable(), getContractAgreementTable(), getContractNegotiationTable(), getContractAgreementIdFkColumn(), getContractAgreementIdColumn());
    }

    @Override
    public SqlQueryStatement createNegotiationsQuery(QuerySpec querySpec) {
        // for generic SQL, only the limit and offset fields are used!
        var sql = getSelectNegotiationsTemplate();
        var stmt = new SqlQueryStatement(sql);
        stmt.addParameter(querySpec.getLimit());
        stmt.addParameter(querySpec.getOffset());
        return stmt;
    }

    @Override
    public SqlQueryStatement createAgreementsQuery(QuerySpec querySpec) {
        // for generic SQL, only the limit and offset fields are used!
        var sql = "SELECT * FROM " + getContractAgreementTable();
        var stmt = new SqlQueryStatement(sql);
        stmt.addParameter(querySpec.getLimit());
        stmt.addParameter(querySpec.getOffset());
        return stmt;
    }

    @Override
    public String getDeleteLeaseTemplate() {
        return format("DELETE FROM %s WHERE %s=?", getLeaseTableName(), getLeaseIdColumn());
    }

    @Override
    public String getInsertLeaseTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?);",
                getLeaseTableName(), getLeaseIdColumn(), getLeasedByColumn(), getLeasedAtColumn(), getLeaseDurationColumn());
    }

    @Override
    public String getUpdateLeaseTemplate() {
        return format("UPDATE %s SET %s=? WHERE %s = ?;", getContractNegotiationTable(), getLeaseIdColumn(), getIdColumn());
    }

    @Override
    public String getFindLeaseByEntityTemplate() {
        return format("SELECT * FROM %s  WHERE %s = (SELECT lease_id FROM %s WHERE %s=? )",
                getLeaseTableName(), getLeaseIdColumn(), getContractNegotiationTable(), getIdColumn());
    }

    /**
     * Overridable operator to convert strings to JSON. For postgres, this is the "::json" operator
     */
    protected String getFormatJsonOperator() {
        return BaseSqlDialect.getJsonCastOperator();
    }

}
