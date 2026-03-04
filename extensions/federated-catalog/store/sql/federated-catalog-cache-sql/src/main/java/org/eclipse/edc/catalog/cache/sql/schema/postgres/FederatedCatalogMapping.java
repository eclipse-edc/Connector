/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.cache.sql.schema.postgres;

import org.eclipse.edc.catalog.cache.sql.FederatedCatalogCacheStatements;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;

public class FederatedCatalogMapping extends TranslationMapping {

    public FederatedCatalogMapping(FederatedCatalogCacheStatements statements) {
        add("id", statements.getIdColumn());
        add("participantId", new JsonFieldTranslator(statements.getCatalogColumn()));
        add("properties", new PrefixedJsonFieldTranslator(statements.getCatalogColumn(), "properties"));
        add("datasets", new JsonFieldTranslator("datasets"));
        add("dataServices", new JsonFieldTranslator("dataServices"));
    }
}
