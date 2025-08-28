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

package org.eclipse.edc.sql.lease.spi;

import org.eclipse.edc.sql.statement.SqlStatements;

import static java.lang.String.format;
import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * Encapsulates statements and table/column names to manipulate lease entities.
 */
public interface LeaseStatements extends SqlStatements {

    default String getDeleteLeaseTemplate() {
        return executeStatement().delete(getLeaseTableName(), criterion(getResourceIdColumn(), "=", "?"), criterion(getResourceKind(), "=", "?"));
    }

    default String getUpsertLeaseTemplate() {
        return executeStatement()
                .column(getResourceIdColumn())
                .column(getLeasedByColumn())
                .column(getResourceKind())
                .column(getLeasedAtColumn())
                .column(getLeaseDurationColumn())
                .upsertInto(getLeaseTableName(), format("%s, %s", getResourceIdColumn(), getResourceKind()), "%s.%s + %s.%s < ?".formatted(getLeaseTableName(), getLeasedAtColumn(), getLeaseTableName(), getLeaseDurationColumn()));
    }

    default String getFindLeaseByEntityTemplate() {
        return "SELECT * FROM %s WHERE %s = ? and %s = ?".formatted(getLeaseTableName(), getResourceIdColumn(), getResourceKind());
    }

    default String getLeaseTableName() {
        return "edc_lease";
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

    default String getResourceKind() {
        return "resource_kind";
    }

    default String getResourceIdColumn() {
        return "resource_id";
    }

}
