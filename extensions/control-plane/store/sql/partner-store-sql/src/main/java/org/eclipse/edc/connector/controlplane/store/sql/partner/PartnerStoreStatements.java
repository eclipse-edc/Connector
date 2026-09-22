/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.partner;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Statement templates and column names for the partner table.
 */
public interface PartnerStoreStatements extends SqlStatements {

    default String getPartnerTable() {
        return "edc_partner";
    }

    default String getParticipantContextIdColumn() {
        return "participant_context_id";
    }

    default String getIdColumn() {
        return "id";
    }

    default String getIdentityColumn() {
        return "identity";
    }

    default String getNameColumn() {
        return "name";
    }

    default String getPropertiesColumn() {
        return "properties";
    }

    default String getGroupIdsColumn() {
        return "group_ids";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    String getInsertTemplate();

    String getUpdateTemplate();

    String getDeleteByIdTemplate();

    String getFindByIdTemplate();

    String getFindByIdentityTemplate();

    String getSelectStatement();

    SqlQueryStatement createQuery(QuerySpec querySpec);
}
