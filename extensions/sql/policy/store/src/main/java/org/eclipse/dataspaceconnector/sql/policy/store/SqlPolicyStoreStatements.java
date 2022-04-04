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

/**
 * Statement templates and SQL table+column names required for the PolicyStore.
 */
public interface SqlPolicyStoreStatements {

    /**
     * SELECT all statement for policies.
     */
    String getSqlFindClauseTemplate();

    /**
     * INSERT statement for policy.
     */
    String getSqlInsertClauseTemplate();

    /**
     * UPDATE statement for policy.
     */
    String getSqlUpdateClauseTemplate();

    /**
     * SELECT statement with condition.
     */
    String getSqlFindByClauseTemplate();

    /**
     * DELETE statement for policies.
     */
    String getSqlDeleteClauseTemplate();


    default String getPolicyTableName() {
        return "edc_policies";
    }

    default String getPolicyColumnId() {
        return "policy_id";
    }

    default String getPolicyColumnPermissions() {
        return "permissions";
    }

    default String getPolicyColumnProhibitions() {
        return "prohibitions";
    }

    default String getPolicyColumnDuties() {
        return "duties";
    }

    default String getPolicyColumnExtensibleProperties() {
        return "extensible_properties";
    }

    default String getPolicyColumnInheritsFrom() {
        return "inherits_from";
    }

    default String getPolicyColumnAssigner() {
        return "assigner";
    }

    default String getPolicyColumnAssignee() {
        return "assignee";
    }

    default String getPolicyColumnTarget() {
        return "target";
    }

    default String getPolicyColumnPolicyType() {
        return "policy_type";
    }

}