/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *       Microsoft Corporation - refactoring
 *
 */

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;

/**
 * Defines all statements that are needed for the ContractDefinition store
 */
public interface ContractDefinitionStatements {
    default String getContractPolicyColumn() {
        return "contract_policy";
    }

    default String getSelectorExpressionColumn() {
        return "selector_expression";
    }

    default String getAccessPolicyColumn() {
        return "access_policy";
    }

    default String getContractDefinitionTable() {
        return "edc_contract_definitions";
    }

    default String getIdColumn() {
        return "contract_definition_id";
    }

    String getDeleteByIdTemplate();

    String getSelectAllTemplate();

    String getFindByTemplate();

    String getInsertTemplate();

    String getCountTemplate();

    String getUpdateTemplate();
}
