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

package org.eclipse.edc.connector.dataplane.selector.store.sql.schema;

import org.eclipse.edc.sql.dialect.BaseSqlDialect;

public interface DataPlaneInstanceStatements {


    default String getDataPlaneInstanceTable() {
        return "edc_data_plane_instance";
    }

    default String getIdColumn() {
        return "id";
    }

    default String getDataColumn() {
        return "data";
    }

    String getFindByIdTemplate();

    String getAllTemplate();

    String getInsertTemplate();

    String getUpdateTemplate();


    default String getFormatAsJsonOperator() {
        return BaseSqlDialect.getJsonCastOperator();
    }

}

