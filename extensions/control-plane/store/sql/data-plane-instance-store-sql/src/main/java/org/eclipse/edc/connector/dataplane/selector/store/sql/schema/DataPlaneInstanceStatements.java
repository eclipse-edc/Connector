/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector.store.sql.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Statement templates and column names of the SQL {@code DataPlaneInstance} store.
 */
public interface DataPlaneInstanceStatements extends SqlStatements {

    default String getDataPlaneInstanceTable() {
        return "edc_data_plane_instance";
    }

    default String getIdColumn() {
        return "id";
    }

    default String getStateColumn() {
        return "state";
    }

    default String getStateTimestampColumn() {
        return "state_time_stamp";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    default String getUpdatedAtColumn() {
        return "updated_at";
    }

    default String getUrlColumn() {
        return "url";
    }

    default String getLastActiveColumn() {
        return "last_active";
    }

    default String getParticipantContextIdColumn() {
        return "participant_context_id";
    }

    default String getAllowedSourceTypesColumn() {
        return "allowed_source_types";
    }

    default String getAllowedTransferTypesColumn() {
        return "allowed_transfer_types";
    }

    default String getDestinationProvisionTypesColumn() {
        return "destination_provision_types";
    }

    default String getLabelsColumn() {
        return "labels";
    }

    default String getPropertiesColumn() {
        return "properties";
    }

    default String getAuthorizationProfileColumn() {
        return "authorization_profile";
    }

    String getFindByIdTemplate();

    String getAllTemplate();

    String getUpsertTemplate();

    String getSelectTemplate();

    String getDeleteByIdTemplate();

    SqlQueryStatement createQuery(QuerySpec querySpec);
}
