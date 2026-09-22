/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.partner.schema.postgres;

import org.eclipse.edc.connector.controlplane.store.sql.partner.PartnerStoreStatements;
import org.eclipse.edc.sql.translation.JsonArrayTranslator;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps {@link org.eclipse.edc.connector.controlplane.partner.spi.Partner} fields to columns. Nested properties are
 * queryable as {@code properties.<key>}, group membership with {@code groupIds contains <groupId>}.
 */
public class PartnerMapping extends TranslationMapping {

    public PartnerMapping(PartnerStoreStatements statements) {
        add("id", statements.getIdColumn());
        add("participantContextId", statements.getParticipantContextIdColumn());
        add("identity", statements.getIdentityColumn());
        add("name", statements.getNameColumn());
        add("createdAt", statements.getCreatedAtColumn());
        add("properties", new JsonFieldTranslator(statements.getPropertiesColumn()));
        add("groupIds", new JsonArrayTranslator(statements.getGroupIdsColumn()));
    }
}
