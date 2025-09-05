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

package org.eclipse.edc.connector.policy.monitor.store.sql.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.lease.StatefulEntityStatements;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

public interface PolicyMonitorStatements extends StatefulEntityStatements, SqlStatements {

    default String getPolicyMonitorTable() {
        return "edc_policy_monitor";
    }

    default String getIdColumn() {
        return "entry_id";
    }

    default String getContractIdColumn() {
        return "contract_id";
    }

    String getUpsertTemplate();

    String getSelectTemplate();

    SqlQueryStatement createQuery(QuerySpec querySpec);

    SqlQueryStatement createNextNotLeaseQuery(QuerySpec querySpec);
}
