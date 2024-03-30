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

package org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.postgres;

import org.eclipse.edc.sql.translation.JsonFieldTranslator;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link ProvisionedResourceSetMapping} onto the corresponding SQL schema (= column names) using Post
 */
class ProvisionedResourceSetMapping extends TranslationMapping {

    private static final String FIELD_RESOURCES = "resources";

    ProvisionedResourceSetMapping() {
        add(FIELD_RESOURCES, new JsonFieldTranslator("resources"));
    }
}
