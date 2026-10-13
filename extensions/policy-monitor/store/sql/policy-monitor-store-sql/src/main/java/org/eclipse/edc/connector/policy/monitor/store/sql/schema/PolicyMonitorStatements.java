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

    default String getLeasedByColumn() {
        return "leased_by";
    }

    default String getLeasedAtColumn() {
        return "leased_at";
    }

    default String getLeaseDurationColumn() {
        return "lease_duration";
    }

    String getUpsertTemplate();

    String getSelectTemplate();

    /**
     * Template for acquiring the lease of an entry, it succeeds only if the entry is not leased or the lease is expired.
     * Parameters: leased by, leased at, lease duration, entry id, now.
     */
    String getAcquireLeaseTemplate();

    /**
     * Template for breaking the lease of an entry, it succeeds only if the entry is not leased, is leased by the given
     * holder or the lease is expired. Parameters: entry id, leased by, now.
     */
    String getBreakLeaseTemplate();

    SqlQueryStatement createQuery(QuerySpec querySpec);

    /**
     * Creates a query that selects and locks (skipping already locked rows) the entries that are not leased at the given time.
     */
    SqlQueryStatement createNextNotLeaseQuery(QuerySpec querySpec, long now);
}
