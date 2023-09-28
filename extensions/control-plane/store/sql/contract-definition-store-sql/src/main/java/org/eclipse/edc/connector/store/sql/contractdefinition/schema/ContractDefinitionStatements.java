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
 *       SAP SE - add private properties to contract definition
 *
 */

package org.eclipse.edc.connector.store.sql.contractdefinition.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines all statements that are needed for the ContractDefinition store
 */
public interface ContractDefinitionStatements extends SqlStatements {
    default String getContractPolicyIdColumn() {
        return "contract_policy_id";
    }

    default String getAssetsSelectorColumn() {
        return "assets_selector";
    }

    default String getAssetsSelectorAlias() {
        return getAssetsSelectorColumn() + "_alias";
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

    default String getCreatedAtColumn() {
        return "created_at";
    }

    default String getContractDefinitionPropertyTable() {
        return "edc_contract_definition_property";
    }

    default String getContractDefinitionPropertyNameColumn() {
        return "property_name";
    }

    default String getContractDefinitionPropertyValueColumn() {
        return "property_value";
    }

    default String getContractDefinitionPropertyTypeColumn() {
        return "property_type";
    }

    default String getContractDefinitionPropertyIsPrivateColumn() {
        return "property_is_private";
    }

    default String getPropertyContractDefinitionIdFkColumn() {
        return "contract_definition_id_fk";
    }

    String getDeleteByIdTemplate();

    String getDeletePropertyByIdTemplate();

    String getFindByTemplate();

    String getInsertTemplate();

    String getInsertPropertyTemplate();

    String getCountTemplate();

    String getFindPropertyByIdTemplate();

    String getUpdateTemplate();

    SqlQueryStatement createQuery(QuerySpec querySpec);

}
