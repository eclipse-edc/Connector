/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.store.sql.schema;

import org.eclipse.edc.sql.dialect.BaseSqlDialect;

/**
 * Sql Statements for DataPlane Store
 */
public interface DataPlaneStatements {

    default String getDataPlaneTable() {
        return "edc_data_plane";
    }

    default String getProcessIdColumn() {
        return "process_id";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    default String getUpdatedAtColumn() {
        return "updated_at";
    }

    default String getStateColumn() {
        return "state";
    }

    String getFindByIdTemplate();

    String getInsertTemplate();

    String getUpdateTemplate();


    default String getFormatAsJsonOperator() {
        return BaseSqlDialect.getJsonCastOperator();
    }

}

