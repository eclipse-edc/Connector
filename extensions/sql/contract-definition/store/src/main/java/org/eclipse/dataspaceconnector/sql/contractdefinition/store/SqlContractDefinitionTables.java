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
 *
 */

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;

public interface SqlContractDefinitionTables {
    String CONTRACT_DEFINITION_TABLE = "edc_contract_definitions";
    String CONTRACT_DEFINITION_COLUMN_ID = "contract_definition_id";
    String CONTRACT_DEFINITION_COLUMN_ACCESS_POLICY = "access_policy";
    String CONTRACT_DEFINITION_COLUMN_CONTRACT_POLICY = "contract_policy";
    String CONTRACT_DEFINITION_COLUMN_SELECTOR = "selector_expression";
}
