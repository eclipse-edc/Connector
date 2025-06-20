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

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.lease.LeaseStatements;
import org.eclipse.edc.sql.lease.StatefulEntityStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

public interface DataPlaneInstanceStatements extends StatefulEntityStatements, LeaseStatements {

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

    String getUpsertTemplate();

    String getSelectTemplate();

    String getDeleteByIdTemplate();

    SqlQueryStatement createQuery(QuerySpec querySpec);

}

