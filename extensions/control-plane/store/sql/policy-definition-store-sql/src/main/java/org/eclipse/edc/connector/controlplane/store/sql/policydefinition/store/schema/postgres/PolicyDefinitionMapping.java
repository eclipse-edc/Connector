/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.controlplane.store.sql.policydefinition.store.schema.postgres;

import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.store.sql.policydefinition.store.schema.SqlPolicyStoreStatements;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link PolicyDefinition} onto the corresponding SQL
 * schema (= column names) enabling access through Postgres JSON operators where applicable
 */
public class PolicyDefinitionMapping extends TranslationMapping {
    public PolicyDefinitionMapping(SqlPolicyStoreStatements statements) {
        add("uid", statements.getPolicyIdColumn());
        add("id", statements.getPolicyIdColumn());
        add("createdAt", statements.getCreatedAtColumn());
        add("policy", new PolicyMapping(statements));
        add("privateProperties", new JsonFieldTranslator(statements.getPrivatePropertiesColumn()));
        add("participantContextId", statements.getParticipantContextIdColumn());
    }
}
