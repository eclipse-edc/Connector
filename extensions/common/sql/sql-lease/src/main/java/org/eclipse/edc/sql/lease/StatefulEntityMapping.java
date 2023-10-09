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

import org.eclipse.edc.sql.translation.JsonFieldMapping;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link org.eclipse.edc.spi.entity.StatefulEntity} onto the
 * corresponding SQL schema (= column names) enabling access through Postgres JSON operators where applicable
 */
public class StatefulEntityMapping extends TranslationMapping {

    protected StatefulEntityMapping(StatefulEntityStatements statements) {
        add("id", statements.getIdColumn());
        add("state", statements.getStateColumn());
        add("stateCount", statements.getStateCountColumn());
        add("createdAt", statements.getCreatedAtColumn());
        add("traceContext", new JsonFieldMapping(statements.getTraceContextColumn()));
        add("errorDetail", statements.getErrorDetailColumn());
    }
}
