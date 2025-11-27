/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.store.sql.participantcontext.config;

import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.sql.statement.SqlStatements;

/**
 * Defines SQL-statements and column names for use with a SQL-based {@link Config}
 */
public interface ParticipantContextConfigStoreStatements extends SqlStatements {

    default String getParticipantContextConfigTable() {
        return "edc_participant_context_config";
    }

    default String getIdColumn() {
        return "participant_context_id";
    }

    default String getCreateTimestampColumn() {
        return "created_date";
    }

    default String getLastModifiedTimestampColumn() {
        return "last_modified_date";
    }

    default String getEntriesColumn() {
        return "entries";
    }

    default String getPrivateEntriesColumn() {
        return "private_entries";
    }

    String getUpsertTemplate();

    String getFindByIdTemplate();

}
