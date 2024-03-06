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

package org.eclipse.edc.connector.dataplane.store.sql.schema.postgres;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.store.sql.schema.AccessTokenDataStatements;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link DataFlow} onto the
 * corresponding SQL schema (= column names) enabling access through Postgres JSON operators where applicable
 */
public class AccessTokenDataMapping extends TranslationMapping {

    public AccessTokenDataMapping(AccessTokenDataStatements statements) {
        add("id", statements.getIdColumn());
        add("claimToken", new JsonFieldTranslator(statements.getClaimTokenColumn()));
        add("dataAddress", new JsonFieldTranslator(statements.getDataAddressColumn()));
        add("additionalProperties", new JsonFieldTranslator(statements.getAdditionalPropertiesColumn()));
    }
}
