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

package org.eclipse.dataspaceconnector.sql.lease;

/**
 * Encapsulates statements and table/column names to manipulate lease entities.
 */
public interface LeaseStatements {
    String getDeleteLeaseTemplate();

    String getInsertLeaseTemplate();

    String getUpdateLeaseTemplate();

    String getFindLeaseByEntityTemplate();

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
