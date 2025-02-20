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

package org.eclipse.edc.connector.dataplane.store.sql.schema.postgres;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.store.sql.schema.DataFlowStatements;
import org.eclipse.edc.sql.lease.StatefulEntityMapping;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link DataFlow} onto the
 * corresponding SQL schema (= column names) enabling access through Postgres JSON operators where applicable
 */
public class DataFlowMapping extends StatefulEntityMapping {

    public DataFlowMapping(DataFlowStatements statements) {
        super(statements, state -> DataFlowStates.valueOf(state).code());
        add("transferType", new TransferTypeMapping(statements));
        add("runtimeId", statements.getRuntimeIdColumn());
    }

    private static class TransferTypeMapping extends TranslationMapping {

        TransferTypeMapping(DataFlowStatements statements) {
            add("flowType", statements.getFlowTypeColumn());
        }
    }
}
