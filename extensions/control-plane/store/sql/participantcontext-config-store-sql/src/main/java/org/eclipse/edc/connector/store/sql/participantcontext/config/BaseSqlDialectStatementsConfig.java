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

import java.util.List;

import static java.lang.String.format;

public class BaseSqlDialectStatementsConfig implements ParticipantContextConfigStoreStatements {

    @Override
    public String getUpsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getCreateTimestampColumn())
                .column(getLastModifiedTimestampColumn())
                .jsonColumn(getEntriesColumn())
                .upsertInto(getParticipantContextConfigTable(), getIdColumn(), List.of(getCreateTimestampColumn()));
    }

    @Override
    public String getFindByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getParticipantContextConfigTable(), getIdColumn());

    }

}
