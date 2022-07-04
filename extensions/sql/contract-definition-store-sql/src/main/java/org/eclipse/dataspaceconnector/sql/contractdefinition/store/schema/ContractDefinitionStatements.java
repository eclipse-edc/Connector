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
import org.eclipse.dataspaceconnector.sql.dialect.BaseSqlDialect;
import org.eclipse.dataspaceconnector.sql.translation.SqlQueryStatement;

/**
 * Defines all statements that are needed for the ContractDefinition store
 */
public interface ContractDefinitionStatements {
    default String getContractPolicyIdColumn() {
        return "contract_policy_id";
    }

    default String getSelectorExpressionColumn() {
        return "selector_expression";
    }

    default String getAccessPolicyIdColumn() {
        return "access_policy_id";
    }

    default String getContractDefinitionTable() {
        return "edc_contract_definitions";
    }

    default String getIdColumn() {
        return "contract_definition_id";
    }

    String getDeleteByIdTemplate();

    String getFindByTemplate();

    String getInsertTemplate();

    String getCountTemplate();

    String getUpdateTemplate();

    SqlQueryStatement createQuery(QuerySpec querySpec);

    default String getFormatAsJsonOperator() {
        return BaseSqlDialect.getJsonCastOperator();
    }
}
