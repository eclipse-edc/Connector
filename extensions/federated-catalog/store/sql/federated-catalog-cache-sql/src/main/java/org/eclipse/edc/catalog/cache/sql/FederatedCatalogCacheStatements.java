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

package org.eclipse.edc.catalog.cache.sql;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

public interface FederatedCatalogCacheStatements extends SqlStatements {

    default String getFederatedCatalogTable() {
        return "edc_federated_catalog";
    }

    default String getIdColumn() {
        return "id";
    }

    default String getCatalogColumn() {
        return "catalog";
    }

    default String getMarkedColumn() {
        return "marked";
    }

    String getFindByIdTemplate();

    String getUpdateAsMarkedTemplate();

    String getDeleteByMarkedTemplate();


    String getInsertTemplate();

    String getUpdateTemplate();

    SqlQueryStatement createQuery(QuerySpec query);

    String getSelectStatement();
}
