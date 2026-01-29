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

package org.eclipse.edc.policy.cel.store.sql.postgres;

import org.eclipse.edc.policy.cel.store.sql.CelExpressionStoreStatements;
import org.eclipse.edc.sql.translation.TranslationMapping;


/**
 * Provides a mapping from the canonical format to SQL column names for a {@code VerifiableCredentialResource}
 */
public class CelExpressionMapping extends TranslationMapping {

    public static final String FIELD_ID = "id";
    public static final String FIELD_CREATE_TIMESTAMP = "createdAt";
    public static final String FIELD_LEFT_OPERAND = "leftOperand";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_EXPRESSION = "expression";
    public static final String FIELD_LASTMODIFIED_TIMESTAMP = "lastModified";

    public CelExpressionMapping(CelExpressionStoreStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_LEFT_OPERAND, statements.getLeftOperandColumn());
        add(FIELD_DESCRIPTION, statements.getDescriptionColumn());
        add(FIELD_EXPRESSION, statements.getExpressionColumn());
        add(FIELD_CREATE_TIMESTAMP, statements.getCreateTimestampColumn());
        add(FIELD_LASTMODIFIED_TIMESTAMP, statements.getLastModifiedTimestampColumn());
    }
}