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

package org.eclipse.edc.connector.dataplane.store.sql.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Sql Statements for DataPlane Store
 */
public interface AccessTokenDataStatements extends SqlStatements {

    default String getIdColumn() {
        return "id";
    }

    default String getTableName() {
        return "edc_accesstokendata";
    }

    default String getClaimTokenColumn() {
        return "claim_token";
    }

    default String getDataAddressColumn() {
        return "data_address";
    }

    default String getAdditionalPropertiesColumn() {
        return "additional_properties";
    }

    String getInsertTemplate();

    String getSelectTemplate();

    String getDeleteTemplate();

    SqlQueryStatement createQuery(QuerySpec querySpec);

}

