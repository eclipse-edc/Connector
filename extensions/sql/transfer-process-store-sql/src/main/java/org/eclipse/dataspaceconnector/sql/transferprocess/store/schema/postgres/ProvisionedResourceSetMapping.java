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

package org.eclipse.dataspaceconnector.sql.transferprocess.store.schema.postgres;

import org.eclipse.dataspaceconnector.sql.translation.JsonFieldMapping;
import org.eclipse.dataspaceconnector.sql.translation.TranslationMapping;

class ProvisionedResourceSetMapping extends TranslationMapping {

    private static final String FIELD_RESOURCES = "resources";

    ProvisionedResourceSetMapping() {
        add(FIELD_RESOURCES, new JsonFieldMapping("resources"));
    }
}
