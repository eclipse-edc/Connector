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

package org.eclipse.edc.jsonld.cache.store.sql.schema;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Statement templates and table/column names for the cached JSON-LD context store.
 */
@ExtensionPoint
public interface CachedJsonLdContextStoreStatements extends SqlStatements {

    String getSelectTemplate();

    String getInsertTemplate();

    String getUpdateTemplate();

    String getDeleteTemplate();

    default String getTable() {
        return "edc_json_ld_context_cache";
    }

    default String getIdColumn() {
        return "id";
    }

    default String getUrlColumn() {
        return "url";
    }

    default String getContentColumn() {
        return "content";
    }

    default String getPullStrategyColumn() {
        return "pull_strategy";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    default String getUpdatedAtColumn() {
        return "updated_at";
    }

    SqlQueryStatement createQuery(QuerySpec querySpec);
}
