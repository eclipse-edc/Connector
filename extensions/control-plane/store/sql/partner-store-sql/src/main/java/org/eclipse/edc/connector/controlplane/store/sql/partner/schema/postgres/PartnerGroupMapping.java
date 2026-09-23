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

import org.eclipse.edc.connector.controlplane.store.sql.partner.PartnerGroupStoreStatements;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps {@link org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup} fields to columns.
 */
public class PartnerGroupMapping extends TranslationMapping {

    public PartnerGroupMapping(PartnerGroupStoreStatements statements) {
        add("id", statements.getIdColumn());
        add("participantContextId", statements.getParticipantContextIdColumn());
        add("name", statements.getNameColumn());
        add("description", statements.getDescriptionColumn());
        add("createdAt", statements.getCreatedAtColumn());
        add("properties", new JsonFieldTranslator(statements.getPropertiesColumn()));
    }
}
