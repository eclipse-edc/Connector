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

package org.eclipse.edc.sql.lease;

import static java.lang.String.format;

/**
 * Encapsulates statements and table/column names to manipulate lease entities.
 */
public interface LeaseStatements {
    String getDeleteLeaseTemplate();

    String getInsertLeaseTemplate();

    String getUpdateLeaseTemplate();

    String getFindLeaseByEntityTemplate();

    default String getNotLeasedFilter() {
        return format("(%s IS NULL OR %s IN (SELECT %s FROM %s WHERE (? > (%s + %s))))",
                getLeaseIdColumn(), getLeaseIdColumn(), getLeaseIdColumn(),
                getLeaseTableName(), getLeasedAtColumn(), getLeaseDurationColumn());
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

    default String getLeaseIdColumn() {
        return "lease_id";
    }

}
