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

package org.eclipse.dataspaceconnector.sql.contractnegotiation.store.schema;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.sql.dialect.BaseSqlDialect;
import org.eclipse.dataspaceconnector.sql.translation.SqlQueryStatement;

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
    public String getFindContractAgreementByDefinitionIdTemplate() {
        return format("SELECT * FROM %s where %s LIKE ?", getContractAgreementTable(), getContractAgreementIdColumn());
    }

    @Override
    public String getUpdateNegotiationTemplate() {
        return format("UPDATE %s\n" +
                        "SET %s=?,\n" +
                        "    %s=?,\n" +
                        "    %s=?,\n" +
                        "    %s=?,\n" +
                        "    %s=?%s,\n" +
                        "    %s=?%s,\n" +
                        "    %s=?\n" +
                        "WHERE id = ?;", getContractNegotiationTable(), getStateColumn(), getStateCountColumn(), getStateTimestampColumn(),
                getErrorDetailColumn(), getContractOffersColumn(), getFormatJsonOperator(), getTraceContextColumn(), getFormatJsonOperator(), getContractAgreementIdFkColumn());
    }

    @Override
    public String getInsertNegotiationTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)\n" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? %s, ?%s); ",
                getContractNegotiationTable(), getIdColumn(), getCorrelationIdColumn(), getCounterPartyIdColumn(), getCounterPartyAddressColumn(), getTypeColumn(), getProtocolColumn(), getStateColumn(), getStateCountColumn(),
                getStateTimestampColumn(), getErrorDetailColumn(), getContractAgreementIdFkColumn(), getContractOffersColumn(), getTraceContextColumn(), getFormatJsonOperator(), getFormatJsonOperator()
        );
    }

    @Override
    public String getDeleteTemplate() {
        return format("DELETE FROM %s WHERE %s = ? AND %s IS NULL;", getContractNegotiationTable(), getIdColumn(), getContractAgreementIdFkColumn());
    }

    @Override
    public String getNextForStateTemplate() {
        return format("SELECT * FROM %s\n" +
                "WHERE %s=?\n" +
                "  AND (%s IS NULL OR %s IN (SELECT %s FROM %s WHERE (? > (%s + %s))))\n" +
                "LIMIT ?;", getContractNegotiationTable(), getStateColumn(), getLeaseIdColumn(), getLeaseIdColumn(), getLeaseIdColumn(), getLeaseTableName(), getLeasedAtColumn(), getLeaseDurationColumn());
    }

    @Override
    public String getSelectFromAgreementsTemplate() {
        // todo: add WHERE ... AND ... ORDER BY... statements here
        return format("SELECT * FROM %s", getContractAgreementTable());
    }

    @Override
    public String getInsertAgreementTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?%s);",
                getContractAgreementTable(), getContractAgreementIdColumn(), getProviderAgentColumn(), getConsumerAgentColumn(),
                getSigningDateColumn(), getStartDateColumn(), getEndDateColumn(), getAssetIdColumn(), getPolicyColumn(), getFormatJsonOperator());
    }

    @Override
    public String getUpdateAgreementTemplate() {
        return format("UPDATE %s SET %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=? WHERE %s =?",
                getContractAgreementTable(), getProviderAgentColumn(), getConsumerAgentColumn(), getSigningDateColumn(),
                getStartDateColumn(), getEndDateColumn(), getAssetIdColumn(), getPolicyColumn(), getContractAgreementIdColumn());
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
