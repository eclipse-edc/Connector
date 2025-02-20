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

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates;
import org.eclipse.edc.sql.lease.StatefulEntityMapping;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;

/**
 * Maps fields of a {@link org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance} onto the
 * corresponding SQL schema (= column names) enabling access through Postgres JSON operators where applicable
 */
public class DataPlaneInstanceMapping extends StatefulEntityMapping {

    public DataPlaneInstanceMapping(DataPlaneInstanceStatements statements) {
        super(statements, state -> DataPlaneInstanceStates.valueOf(state).code());
        var data = new JsonFieldTranslator(statements.getDataColumn());
        add("state", data);
        add("stateCount", data);
        add("stateTimestamp", data);
        add("createdAt", data);
        add("traceContext", new JsonFieldTranslator(statements.getTraceContextColumn()));
        add("errorDetail", data);
    }

}
