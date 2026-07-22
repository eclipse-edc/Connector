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

package org.eclipse.edc.connector.controlplane.store.sql.dataspaceprofile.store.schema;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Statement templates and SQL table+column names required for the {@code DataspaceProfileStore}.
 */
@ExtensionPoint
public interface DataspaceProfileStoreStatements extends SqlStatements {

    String getSelectTemplate();

    String getInsertTemplate();

    String getUpdateTemplate();

    String getDeleteTemplate();

    default String getProfileTable() {
        return "edc_dataspace_profiles";
    }

    default String getNameColumn() {
        return "name";
    }

    default String getProtocolVersionColumn() {
        return "protocol_version";
    }

    default String getPathColumn() {
        return "path";
    }

    default String getBindingColumn() {
        return "binding";
    }

    default String getNamespaceColumn() {
        return "namespace";
    }

    default String getJsonLdContextsUrlColumn() {
        return "jsonld_contexts_url";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    default String getTrustedIssuersColumn() {
        return "trusted_issuers";
    }

    SqlQueryStatement createQuery(QuerySpec querySpec);
}
