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

import static java.lang.String.format;

class PostgresStatements implements ContractDefinitionStatements {
    @Override
    public String getDeleteByIdTemplate() {
        return format("DELETE FROM %s WHERE %s = ?",
                getContractDefinitionTable(),
                getIdColumn());
    }

    @Override
    public String getSelectAllTemplate() {
        return format("SELECT * from %s LIMIT ? OFFSET ?",
                getContractDefinitionTable());
    }

    @Override
    public String getFindByTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getContractDefinitionTable(), getIdColumn());
    }

    @Override
    public String getInsertTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
                getContractDefinitionTable(),
                getIdColumn(),
                getAccessPolicyIdColumn(),
                getContractPolicyIdColumn(),
                getSelectorExpressionColumn());
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
        return format("UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = ? WHERE %s = ?",
                getContractDefinitionTable(),
                getIdColumn(),
                getAccessPolicyIdColumn(),
                getContractPolicyIdColumn(),
                getSelectorExpressionColumn(),
                getIdColumn());
    }

    @Override
    public String getIsPolicyReferencedTemplate() {
        return format("SELECT * FROM %s WHERE (%s=? OR %s=?);", getContractDefinitionTable(), getAccessPolicyIdColumn(), getContractPolicyIdColumn());
    }
}
