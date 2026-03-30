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

package org.eclipse.edc.controlplane.tasks.sql.schema.postgres;

import org.eclipse.edc.controlplane.tasks.sql.TaskStatements;
import org.eclipse.edc.sql.translation.TranslationMapping;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@code Task}
 */
public class TaskMapping extends TranslationMapping {

    public static final String FIELD_ID = "id";
    public static final String FIELD_AT_TIMESTAMP = "at";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_GROUP = "group";

    public TaskMapping(TaskStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_AT_TIMESTAMP, statements.getTimestampColumn());
        add(FIELD_NAME, statements.getNameColumn());
        add(FIELD_GROUP, statements.getGroupColumn());
    }
}