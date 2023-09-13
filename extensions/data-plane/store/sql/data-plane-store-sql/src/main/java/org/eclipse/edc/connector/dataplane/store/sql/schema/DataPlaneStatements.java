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

package org.eclipse.edc.connector.dataplane.store.sql.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.lease.LeaseStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Sql Statements for DataPlane Store
 */
public interface DataPlaneStatements extends LeaseStatements {

    default String getIdColumn() {
        return "process_id";
    }

    default String getDataPlaneTable() {
        return "edc_data_plane";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    default String getUpdatedAtColumn() {
        return "updated_at";
    }

    default String getStateColumn() {
        return "state";
    }

    default String getTraceContextColumn() {
        return "trace_context";
    }

    default String getStateCountColumn() {
        return "state_count";
    }

    default String getStateTimestampColumn() {
        return "state_time_stamp";
    }

    default String getErrorDetailColumn() {
        return "error_detail";
    }

    default String getCallbackAddressColumn() {
        return "callback_address";
    }

    default String getTrackableColumn() {
        return "trackable";
    }

    default String getSourceColumn() {
        return "source";
    }

    default String getDestinationColumn() {
        return "destination";
    }

    default String getPropertiesColumn() {
        return "properties";
    }

    String getFindByIdTemplate();

    String getInsertTemplate();

    String getUpdateTemplate();

    String getSelectTemplate();

    SqlQueryStatement createQuery(QuerySpec querySpec);

}

