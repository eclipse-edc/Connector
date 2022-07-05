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

package org.eclipse.dataspaceconnector.sql.policy.store.schema;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.sql.policy.store.schema.postgres.PolicyDefinitionMapping;
import org.eclipse.dataspaceconnector.sql.translation.SqlQueryStatement;

public class BaseSqlDialectStatements implements SqlPolicyStoreStatements {

    @Override
    public String getSelectTemplate() {
        return String.format("SELECT * FROM %s",
                getPolicyTable());
    }

    @Override
    public String getInsertTemplate() {
        return String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?%s, ?%s, ?%s, ?%s, ?, ?, ?, ?, ?)",
                getPolicyTable(),
                // keys
                getPolicyIdColumn(),
                getPermissionsColumn(),
                getProhibitionsColumn(),
                getDutiesColumn(),
                getExtensiblePropertiesColumn(),
                getInheritsFromColumn(),
                getAssignerColumn(),
                getAssigneeColumn(),
                getTargetColumn(),
                getTypeColumn(),
                getFormatAsJsonOperator(), getFormatAsJsonOperator(), getFormatAsJsonOperator(), getFormatAsJsonOperator()
        );
    }

    @Override
    public String getUpdateTemplate() {
        return String.format("UPDATE %s SET %s=?%s, %s=?%s, %s=?%s, %s=?%s, %s=?, %s=?, %s=?, %s=?, %s=? WHERE %s=?",
                getPolicyTable(),
                getPermissionsColumn(), getFormatAsJsonOperator(),
                getProhibitionsColumn(), getFormatAsJsonOperator(),
                getDutiesColumn(), getFormatAsJsonOperator(),
                getExtensiblePropertiesColumn(), getFormatAsJsonOperator(),
                getInheritsFromColumn(),
                getAssignerColumn(),
                getAssigneeColumn(),
                getTargetColumn(),
                getTypeColumn(),
                getPolicyIdColumn());
    }

    @Override
    public String getDeleteTemplate() {
        return String.format("DELETE FROM %s WHERE %s = ?",
                getPolicyTable(),
                getPolicyIdColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new PolicyDefinitionMapping(this));
    }
}
