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

package org.eclipse.edc.sql.lease;

public interface StatefulEntityStatements {

    String getIdColumn();

    default String getStateColumn() {
        return "state";
    }

    default String getStateTimestampColumn() {
        return "state_time_stamp";
    }

    default String getStateCountColumn() {
        return "state_count";
    }

    default String getTraceContextColumn() {
        return "trace_context";
    }

    default String getErrorDetailColumn() {
        return "error_detail";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    default String getUpdatedAtColumn() {
        return "updated_at";
    }
}
