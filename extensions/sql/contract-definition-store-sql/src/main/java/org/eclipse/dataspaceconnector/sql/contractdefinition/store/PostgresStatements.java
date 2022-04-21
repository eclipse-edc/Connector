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

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;

class PostgresStatements implements ContractDefinitionStatements {
    @Override
    public String getDeleteByIdTemplate() {
        return String.format("DELETE FROM %s WHERE %s = ?",
                getContractDefinitionTable(),
                getIdColumn());
    }

    @Override
    public String getSelectAllTemplate() {
        return String.format("SELECT * from %s LIMIT ? OFFSET ?",
                getContractDefinitionTable());
    }

    @Override
    public String getFindByTemplate() {
        return String.format("SELECT * FROM %s WHERE %s = ?", getContractDefinitionTable(), getIdColumn());
    }

    @Override
    public String getInsertTemplate() {
        return String.format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
                getContractDefinitionTable(),
                getIdColumn(),
                getAccessPolicyColumn(),
                getContractPolicyColumn(),
                getSelectorExpressionColumn());
    }

    @Override

    public String getCountTemplate() {
        return String.format("SELECT COUNT (%s) FROM %s WHERE %s = ?",
                getIdColumn(),
                getContractDefinitionTable(),
                getIdColumn());
    }

    @Override
    public String getUpdateTemplate() {
        return String.format("UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = ? WHERE %s = ?",
                getContractDefinitionTable(),
                getIdColumn(),
                getAccessPolicyColumn(),
                getContractPolicyColumn(),
                getSelectorExpressionColumn(),
                getIdColumn());
    }
}
