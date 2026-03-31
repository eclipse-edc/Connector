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

package org.eclipse.edc.controlplane.tasks.sql;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines the SQL statements used by the TaskStore. This includes statements for inserting, updating, deleting, and querying tasks.
 */
public interface TaskStatements extends SqlStatements {


    default String getTaskTable() {
        return "edc_tasks";
    }

    default String getIdColumn() {
        return "id";
    }

    default String getNameColumn() {
        return "name";
    }

    default String getPayloadColumn() {
        return "payload";
    }

    default String getTimestampColumn() {
        return "timestamp";
    }

    default String getRetryCountColumn() {
        return "retry_count";
    }

    default String getGroupColumn() {
        return "task_group";
    }

    String getInsertTemplate();

    String getUpdateTemplate();

    String findByIdTemplate();

    String getDeleteStatement();

    SqlQueryStatement createQuery(QuerySpec querySpec);

}
