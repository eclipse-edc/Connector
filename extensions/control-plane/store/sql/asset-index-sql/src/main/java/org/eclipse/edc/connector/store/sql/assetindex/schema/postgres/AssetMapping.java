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

package org.eclipse.edc.connector.store.sql.assetindex.schema.postgres;

import org.eclipse.edc.connector.store.sql.assetindex.schema.AssetStatements;
import org.eclipse.edc.spi.types.PathItem;
import org.eclipse.edc.sql.translation.JsonFieldMapping;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link org.eclipse.edc.spi.types.domain.asset.Asset} onto the
 * corresponding SQL schema (= column names) enabling access through Postgres JSON operators where applicable
 */
public class AssetMapping extends TranslationMapping {

    public AssetMapping(AssetStatements statements) {
        add("id", statements.getAssetIdColumn());
        add("createdAt", statements.getCreatedAtColumn());
        add("properties", new JsonFieldMapping(statements.getPropertiesColumn()));
        add("privateProperties", new JsonFieldMapping(statements.getPrivatePropertiesColumn()));
        add("dataAddress", new JsonFieldMapping(statements.getDataAddressColumn()));
    }

    @Override
    public String getStatement(String canonicalPropertyName, Class<?> type) {
        var standardPath = getStatement(PathItem.parse(canonicalPropertyName), type);

        if (standardPath == null) {
            var amendedCanonicalPropertyName = canonicalPropertyName.contains("'")
                    ? "properties.%s".formatted(canonicalPropertyName)
                    : "properties.'%s'".formatted(canonicalPropertyName);
            return getStatement(amendedCanonicalPropertyName, type);
        }

        return standardPath;
    }

}
