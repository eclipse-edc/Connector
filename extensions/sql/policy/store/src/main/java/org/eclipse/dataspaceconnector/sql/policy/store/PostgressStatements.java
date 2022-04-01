/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.sql.policy.store;

public class PostgressStatements implements SqlPolicyStoreStatements {

    @Override
    public String getSqlFindClauseTemplate() {
        return String.format("SELECT * FROM %s LIMIT ? OFFSET ?",
                getPolicyTableName());
    }

    @Override
    public String getSqlInsertClauseTemplate() {
        return String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                getPolicyTableName(),
                getPolicyColumnId(),
                getPolicyColumnPermissions(),
                getPolicyColumnProhibitions(),
                getPolicyColumnDuties(),
                getPolicyColumnExtensibleProperties(),
                getPolicyColumnInheritsFrom(),
                getPolicyColumnAssigner(),
                getPolicyColumnAssignee(),
                getPolicyColumnTarget(),
                getPolicyColumnPolicyType());
    }

    @Override
    public String getSqlUpdateClauseTemplate() {
        return String.format("UPDATE %s SET %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=? WHERE %s=?",
                getPolicyTableName(),
                getPolicyColumnPermissions(),
                getPolicyColumnProhibitions(),
                getPolicyColumnDuties(),
                getPolicyColumnExtensibleProperties(),
                getPolicyColumnInheritsFrom(),
                getPolicyColumnAssigner(),
                getPolicyColumnAssignee(),
                getPolicyColumnTarget(),
                getPolicyColumnPolicyType(),
                getPolicyColumnId());
    }

    @Override
    public String getSqlFindByClauseTemplate() {
        return String.format("SELECT * FROM %s WHERE %s = ?",
                getPolicyTableName(),
                getPolicyColumnId());
    }

    @Override
    public String getSqlDeleteClauseTemplate() {
        return String.format("DELETE FROM %s WHERE %s = ?",
                getPolicyTableName(),
                getPolicyColumnId());
    }
}
