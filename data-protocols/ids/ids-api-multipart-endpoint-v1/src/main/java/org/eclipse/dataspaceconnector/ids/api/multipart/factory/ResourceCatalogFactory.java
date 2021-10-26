/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.factory;

import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.ResourceCatalogBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Deprecated // This functionality will be moved to a transformer class
public class ResourceCatalogFactory {

    /**
     * The current implementation always creates exactly one IDS Resource Catalog. In case other applications are storing a copy of the catalog (e.g. broker)
     * it is necessary to use a static ID. So that these applications know, it's the same catalog.
     */
    private static final String CONSTANT_RESOURCE_CATALOG_ID = "acf91712-e626-4f20-9f31-f4e241619dcd";

    @NotNull
    public ResourceCatalog createResourceCatalogBuilder(@Nullable List<Resource> resources) {

        var resourceCatalogBuilder = new ResourceCatalogBuilder(IdsId.resourceCatalog(CONSTANT_RESOURCE_CATALOG_ID).toUri());

        if (resources != null) {
            resourceCatalogBuilder._offeredResource_(new ArrayList<>(resources));
        }

        return resourceCatalogBuilder.build();
    }
}
