/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.store.sql.transferprocess.store.schema.postgres;

import org.eclipse.edc.connector.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.sql.translation.JsonFieldMapping;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link ResourceManifest} onto the
 * corresponding SQL schema (= column names) enabling access through Postgres JSON operators
 */
class ResourceManifestMapping extends TranslationMapping {

    private static final String FIELD_DEFINITIONS = "definitions";

    ResourceManifestMapping() {
        add(FIELD_DEFINITIONS, new JsonFieldMapping("definitions"));
    }
}
