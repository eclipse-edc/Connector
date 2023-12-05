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

package org.eclipse.edc.connector.store.sql.contractdefinition.schema.postgres;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.store.sql.contractdefinition.schema.ContractDefinitionStatements;
import org.eclipse.edc.spi.types.PathItem;
import org.eclipse.edc.sql.translation.JsonFieldMapping;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link ContractDefinition} onto the
 * corresponding SQL schema (= column names)
 */
public class ContractDefinitionMapping extends TranslationMapping {
    public ContractDefinitionMapping(ContractDefinitionStatements statements) {
        add("id", statements.getIdColumn());
        add("accessPolicyId", statements.getAccessPolicyIdColumn());
        add("accessPolicy", statements.getAccessPolicyIdColumn());
        add("contractPolicyId", statements.getContractPolicyIdColumn());
        add("contractPolicy", statements.getContractPolicyIdColumn());
        add("assetsSelector", new JsonFieldMapping(statements.getAssetsSelectorAlias()));
        add("privateProperties", new JsonFieldMapping(statements.getPrivatePropertiesColumn()));
    }


    @Override
    public String getStatement(String canonicalPropertyName, Class<?> type) {
        var standardPath = getStatement(PathItem.parse(canonicalPropertyName), type);

        if (standardPath == null) {
            var amendedCanonicalPropertyName = canonicalPropertyName.contains("'")
                    ? "privateProperties.%s".formatted(canonicalPropertyName)
                    : "privateProperties.'%s'".formatted(canonicalPropertyName);
            return getStatement(amendedCanonicalPropertyName, type);
        }

        return standardPath;
    }

}
