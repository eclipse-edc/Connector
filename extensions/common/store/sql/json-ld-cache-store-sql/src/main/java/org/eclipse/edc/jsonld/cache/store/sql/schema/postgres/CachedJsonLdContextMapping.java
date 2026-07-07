/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.cache.store.sql.schema.postgres;

import org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext;
import org.eclipse.edc.jsonld.cache.store.sql.schema.CachedJsonLdContextStoreStatements;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link CachedJsonLdContext} onto the corresponding SQL columns.
 */
public class CachedJsonLdContextMapping extends TranslationMapping {

    public CachedJsonLdContextMapping(CachedJsonLdContextStoreStatements statements) {
        add("id", statements.getIdColumn());
        add("url", statements.getUrlColumn());
        add("content", statements.getContentColumn());
        add("pullStrategy", statements.getPullStrategyColumn());
        add("createdAt", statements.getCreatedAtColumn());
        add("updatedAt", statements.getUpdatedAtColumn());
    }
}
