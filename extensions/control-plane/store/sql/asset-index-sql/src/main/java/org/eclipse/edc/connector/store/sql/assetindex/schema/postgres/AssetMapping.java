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
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;

import java.util.function.Function;

import static java.util.Objects.requireNonNullElse;

/**
 * Maps fields of a {@link org.eclipse.edc.spi.types.domain.asset.Asset} onto the
 * corresponding SQL schema (= column names) enabling access through Postgres JSON operators where applicable
 */
public class AssetMapping extends TranslationMapping {

    public AssetMapping(AssetStatements statements) {
        add("id", statements.getAssetIdColumn());
        add("createdAt", statements.getCreatedAtColumn());
        add("properties", new JsonFieldTranslator(statements.getPropertiesColumn()));
        add("privateProperties", new JsonFieldTranslator(statements.getPrivatePropertiesColumn()));
        add("dataAddress", new JsonFieldTranslator(statements.getDataAddressColumn()));
    }

    @Override
    public Function<Class<?>, String> getFieldTranslator(String fieldPath) {
        return requireNonNullElse(super.getFieldTranslator(fieldPath), fieldPath.contains("'")
                ? super.getFieldTranslator("properties.%s".formatted(fieldPath))
                : super.getFieldTranslator("properties.'%s'".formatted(fieldPath)));
    }

}
