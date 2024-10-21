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

package org.eclipse.edc.edr.store.index.sql.schema;

import org.eclipse.edc.sql.statement.SqlStatements;

/**
 * Defines all statements that are needed for the {@link org.eclipse.edc.jwt.validation.jti.JtiValidationEntry} store
 */
public interface JtiValidationStoreStatements extends SqlStatements {
    default String getTokenIdColumn() {
        return "token_id";
    }

    default String getExpirationTimeColumn() {
        return "expires_at";
    }

    default String getJtiValidationTable() {
        return "edc_jti_validation";
    }

    String getDeleteByIdTemplate();

    String getFindByTemplate();

    String getInsertTemplate();

    String deleteWhereExpiredTemplate();
}
