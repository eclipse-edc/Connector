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

package org.eclipse.edc.iam.identitytrust.sts.store.schema.postgres;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClient;
import org.eclipse.edc.iam.identitytrust.sts.store.schema.StsClientStatements;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link StsClient} onto the
 * corresponding SQL schema (= column names)
 */
public class StsClientMapping extends TranslationMapping {
    public StsClientMapping(StsClientStatements statements) {
        add("id", statements.getIdColumn());
        add("name", statements.getNameColumn());
        add("clientId", statements.getClientIdColumn());
        add("did", statements.getDidColumn());
        add("secretAlias", statements.getSecretAliasColumn());
        add("privateKeyAlias", statements.getPrivateKeyAliasColumn());
        add("publicKeyReference", statements.getPublicKeyReferenceColumn());
        add("createdAt", statements.getCreatedAtColumn());
    }
}
