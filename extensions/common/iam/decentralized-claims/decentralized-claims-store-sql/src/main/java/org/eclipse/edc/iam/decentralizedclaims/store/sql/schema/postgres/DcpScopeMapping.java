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

package org.eclipse.edc.iam.decentralizedclaims.store.sql.schema.postgres;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.store.sql.schema.DcpScopeStatements;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link DcpScope} onto the corresponding SQL schema (= column names).
 */
public class DcpScopeMapping extends TranslationMapping {

    public DcpScopeMapping(DcpScopeStatements statements) {
        add("id", statements.getIdColumn());
        add("type", statements.getTypeColumn());
        add("value", statements.getValueColumn());
        add("profile", statements.getProfileColumn());
        add("prefixMapping", statements.getPrefixMappingColumn());
    }
}
