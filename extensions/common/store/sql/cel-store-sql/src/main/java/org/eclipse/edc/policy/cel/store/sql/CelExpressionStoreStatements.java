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

package org.eclipse.edc.policy.cel.store.sql;

import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines SQL-statements and column names for use with a SQL-based {@link CelExpression}
 */
public interface CelExpressionStoreStatements extends SqlStatements {

    default String getCelExpressionTable() {
        return "edc_cel_expression";
    }

    default String getIdColumn() {
        return "id";
    }

    default String getCreateTimestampColumn() {
        return "created_date";
    }

    default String getLastModifiedTimestampColumn() {
        return "last_modified_date";
    }

    default String getScopesColumn() {
        return "scopes";
    }

    default String getLeftOperandColumn() {
        return "left_operand";
    }

    default String getExpressionColumn() {
        return "expression";
    }

    default String getDescriptionColumn() {
        return "description";
    }

    String getInsertTemplate();

    String getUpdateTemplate();

    String getDeleteByIdTemplate();

    String getFindByIdTemplate();

    SqlQueryStatement createQuery(QuerySpec query);

    String getSelectStatement();
}
