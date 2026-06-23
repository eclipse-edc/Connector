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

package org.eclipse.edc.iam.decentralizedclaims.store.sql.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Sql Statements for the DCP scope store.
 */
public interface DcpScopeStatements extends SqlStatements {

    default String getIdColumn() {
        return "id";
    }

    default String getTypeColumn() {
        return "type";
    }

    default String getValueColumn() {
        return "value";
    }

    default String getProfileColumn() {
        return "profile";
    }

    default String getPrefixMappingColumn() {
        return "prefix_mapping";
    }

    default String getTableName() {
        return "edc_dcp_scope";
    }

    String getInsertTemplate();

    String getUpdateTemplate();

    String getSelectTemplate();

    String getDeleteTemplate();

    SqlQueryStatement createQuery(QuerySpec querySpec);
}
