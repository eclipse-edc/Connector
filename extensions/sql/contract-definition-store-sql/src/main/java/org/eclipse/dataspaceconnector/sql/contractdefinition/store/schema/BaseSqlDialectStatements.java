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

package org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.postgres.ContractDefinitionMapping;
import org.eclipse.dataspaceconnector.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements ContractDefinitionStatements {
    @Override
    public String getDeleteByIdTemplate() {
        return format("DELETE FROM %s WHERE %s = ?",
                getContractDefinitionTable(),
                getIdColumn());
    }

    @Override
    public String getFindByTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getContractDefinitionTable(), getIdColumn());
    }

    @Override
    public String getInsertTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?%s)",
                getContractDefinitionTable(),
                getIdColumn(),
                getAccessPolicyIdColumn(),
                getContractPolicyIdColumn(),
                getSelectorExpressionColumn(),
                getFormatAsJsonOperator());
    }

    @Override

    public String getCountTemplate() {
        return format("SELECT COUNT (%s) FROM %s WHERE %s = ?",
                getIdColumn(),
                getContractDefinitionTable(),
                getIdColumn());
    }

    @Override
    public String getUpdateTemplate() {
        return format("UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = ?%s WHERE %s = ?",
                getContractDefinitionTable(),
                getIdColumn(),
                getAccessPolicyIdColumn(),
                getContractPolicyIdColumn(),
                getSelectorExpressionColumn(),
                getFormatAsJsonOperator(),
                getIdColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = format("SELECT * FROM %s", getContractDefinitionTable());
        return new SqlQueryStatement(select, querySpec, new ContractDefinitionMapping(this));
    }

    protected String getSelectStatement() {
        return "SELECT * FROM " + getContractDefinitionTable();
    }
}
