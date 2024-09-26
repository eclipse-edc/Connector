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

package org.eclipse.edc.iam.identitytrust.sts.store.schema;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Defines all statements that are needed for the {@link StsAccount} store
 */
public interface StsClientStatements extends SqlStatements {

    default String getIdColumn() {
        return "id";
    }

    default String getDidColumn() {
        return "did";
    }

    default String getClientIdColumn() {
        return "client_id";
    }

    default String getStsClientTable() {
        return "edc_sts_client";
    }

    default String getNameColumn() {
        return "name";
    }

    default String getSecretAliasColumn() {
        return "secret_alias";
    }

    default String getPrivateKeyAliasColumn() {
        return "private_key_alias";
    }

    default String getPublicKeyReferenceColumn() {
        return "public_key_reference";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    String getDeleteByIdTemplate();

    String getFindByTemplate();

    String getFindByClientIdTemplate();

    String getInsertTemplate();

    String getUpdateTemplate();

    SqlQueryStatement createQuery(QuerySpec querySpec);

}
