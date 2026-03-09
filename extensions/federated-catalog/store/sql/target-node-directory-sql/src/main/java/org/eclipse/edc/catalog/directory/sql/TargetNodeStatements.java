/*
 *  Copyright (c) 2024 Amadeus IT Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.directory.sql;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

public interface TargetNodeStatements extends SqlStatements {

    default String getTargetNodeDirectoryTable() {
        return "edc_target_node_directory";
    }

    default String getIdColumn() {
        return "id";
    }

    default String getNameColumn() {
        return "name";
    }

    default String getTargetUrlColumn() {
        return "target_url";
    }

    default String getSupportedProtocolsColumn() {
        return "supported_protocols";
    }

    String getInsertTemplate();

    String getFindByIdTemplate();

    String getUpdateTemplate();

    String getDeleteTemplate();

    SqlQueryStatement createQuery(QuerySpec query);

    String getSelectStatement();
}
