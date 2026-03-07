/*
 *  Copyright (c) 2024 Amadeus IT Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.directory.sql.schema.postgres;

import org.eclipse.edc.catalog.directory.sql.TargetNodeStatements;
import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;

public class TargetNodeMapping extends TranslationMapping {

    public TargetNodeMapping(TargetNodeStatements statements) {
        add("id", statements.getIdColumn());
        add("name", statements.getNameColumn());
        add("targetUrl", statements.getTargetUrlColumn());
        add("supportedProtocols", new JsonFieldTranslator(statements.getSupportedProtocolsColumn()));
    }
}
