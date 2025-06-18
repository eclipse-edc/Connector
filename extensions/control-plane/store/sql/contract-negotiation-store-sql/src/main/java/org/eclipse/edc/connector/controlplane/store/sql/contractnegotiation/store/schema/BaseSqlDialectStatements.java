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

package org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;
import static org.eclipse.edc.sql.statement.SqlExecuteStatement.equalTo;
import static org.eclipse.edc.sql.statement.SqlExecuteStatement.isNull;

/**
 * Provides statements required by the ContractNegotiationStore in generic SQL, that is not specific to a particular
 * database. This class is abstract, because there are some statements that cannot be expressed in a generic way.
 */
public class BaseSqlDialectStatements implements ContractNegotiationStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

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
        return executeStatement()
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
                .column(getCorrelationIdColumn())
                .jsonColumn(getProtocolMessagesColumn())
                .update(getContractNegotiationTable(), getIdColumn());
    }

    @Override
    public String getInsertNegotiationTemplate() {
        return executeStatement()
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
                .jsonColumn(getProtocolMessagesColumn())
                .insertInto(getContractNegotiationTable());
    }

    @Override
    public String getDeleteTemplate() {
        return executeStatement()
                .delete(getContractNegotiationTable(), equalTo(getIdColumn()), isNull(getContractAgreementIdFkColumn()));
    }

    @Override
    public String getUpsertNegotiationTemplate() {
        return executeStatement()
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
                .jsonColumn(getProtocolMessagesColumn())
                .upsertInto(getContractNegotiationTable(), getIdColumn());
    }

    @Override
    public String getSelectFromAgreementsTemplate() {
        // todo: add WHERE ... AND ... ORDER BY... statements here
        return format("SELECT * FROM %s", getContractAgreementTable());
    }

    @Override
    public String getUpsertAgreementTemplate() {
        return executeStatement()
                .column(getContractAgreementIdColumn())
                .column(getProviderAgentColumn())
                .column(getConsumerAgentColumn())
                .column(getSigningDateColumn())
                .column(getAssetIdColumn())
                .jsonColumn(getPolicyColumn())
                .upsertInto(getContractAgreementTable(), getContractAgreementIdColumn());
    }

    @Override
    public String getSelectNegotiationsTemplate() {
        return format("SELECT * FROM %s LEFT JOIN %s agr ON %s.%s = agr.%s", getContractNegotiationTable(), getContractAgreementTable(), getContractNegotiationTable(), getContractAgreementIdFkColumn(), getContractAgreementIdColumn());
    }

    @Override
    public SqlQueryStatement createNegotiationsQuery(QuerySpec querySpec) {
        // for generic SQL, only the limit and offset fields are used!
        var sql = getSelectNegotiationsTemplate();
        return new SqlQueryStatement(sql, querySpec.getLimit(), querySpec.getOffset());
    }

    @Override
    public SqlQueryStatement createAgreementsQuery(QuerySpec querySpec) {
        // for generic SQL, only the limit and offset fields are used!
        var sql = "SELECT * FROM " + getContractAgreementTable();
        return new SqlQueryStatement(sql, querySpec.getLimit(), querySpec.getOffset());
    }

    @Override
    public String getDeleteLeaseTemplate() {
        return executeStatement()
                .delete(getLeaseTableName(), getLeaseIdColumn());
    }

    @Override
    public String getInsertLeaseTemplate() {
        return executeStatement()
                .column(getLeaseIdColumn())
                .column(getLeasedByColumn())
                .column(getLeasedAtColumn())
                .column(getLeaseDurationColumn())
                .insertInto(getLeaseTableName());
    }

    @Override
    public String getUpdateLeaseTemplate() {
        return executeStatement()
                .column(getLeaseIdColumn())
                .update(getContractNegotiationTable(), getIdColumn());
    }

    @Override
    public String getFindLeaseByEntityTemplate() {
        return format("SELECT * FROM %s  WHERE %s = (SELECT lease_id FROM %s WHERE %s=? )",
                getLeaseTableName(), getLeaseIdColumn(), getContractNegotiationTable(), getIdColumn());
    }

}
