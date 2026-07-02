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

package org.eclipse.edc.connector.controlplane.store.sql.dataspaceprofile.store.schema.postgres;

import org.eclipse.edc.connector.controlplane.store.sql.dataspaceprofile.store.schema.DataspaceProfileStoreStatements;
import org.eclipse.edc.protocol.spi.DataspaceProfile;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link DataspaceProfile} onto the corresponding SQL columns.
 */
public class DataspaceProfileMapping extends TranslationMapping {
    public DataspaceProfileMapping(DataspaceProfileStoreStatements statements) {
        add("name", statements.getNameColumn());
        add("protocolVersion", statements.getProtocolVersionColumn());
        add("path", statements.getPathColumn());
        add("binding", statements.getBindingColumn());
        add("namespace", statements.getNamespaceColumn());
        add("jsonLdContextsUrl", statements.getJsonLdContextsUrlColumn());
        add("createdAt", statements.getCreatedAtColumn());
    }
}
