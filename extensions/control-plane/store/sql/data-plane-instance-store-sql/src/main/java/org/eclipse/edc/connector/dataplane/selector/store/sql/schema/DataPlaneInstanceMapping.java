/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.selector.store.sql.schema;

import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstanceStates;
import org.eclipse.edc.sql.translation.EntityStateFieldTranslator;
import org.eclipse.edc.sql.translation.JsonArrayTranslator;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link DataPlaneInstance} onto the columns of the SQL table.
 */
public class DataPlaneInstanceMapping extends TranslationMapping {

    public DataPlaneInstanceMapping(DataPlaneInstanceStatements statements) {
        add("id", statements.getIdColumn());
        add("state", new EntityStateFieldTranslator(statements.getStateColumn(), state -> DataPlaneInstanceStates.valueOf(state).code()));
        add("stateTimestamp", statements.getStateTimestampColumn());
        add("createdAt", statements.getCreatedAtColumn());
        add("updatedAt", statements.getUpdatedAtColumn());
        add("url", statements.getUrlColumn());
        add("lastActive", statements.getLastActiveColumn());
        add("participantContextId", statements.getParticipantContextIdColumn());
        add("allowedSourceTypes", new JsonArrayTranslator(statements.getAllowedSourceTypesColumn()));
        add("allowedTransferTypes", new JsonArrayTranslator(statements.getAllowedTransferTypesColumn()));
        add("destinationProvisionTypes", new JsonArrayTranslator(statements.getDestinationProvisionTypesColumn()));
        add("labels", new JsonArrayTranslator(statements.getLabelsColumn()));
        add("properties", new JsonFieldTranslator(statements.getPropertiesColumn()));
        add("authorizationProfile", new JsonFieldTranslator(statements.getAuthorizationProfileColumn()));
    }
}
