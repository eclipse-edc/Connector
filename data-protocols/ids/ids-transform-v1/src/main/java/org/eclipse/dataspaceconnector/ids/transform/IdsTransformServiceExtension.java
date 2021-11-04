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
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Arrays;
import java.util.Set;

public class IdsTransformServiceExtension implements ServiceExtension {
    private static final String NAME = "IDS Transform extension";

    private static final String[] REQUIRES = {
            "edc:ids:core"
    };

    private static final String[] PROVIDES = {
            "edc:ids:transform:v1"
    };

    private Monitor monitor;

    @Override
    public Set<String> requires() {
        return Set.of(REQUIRES);
    }

    @Override
    public Set<String> provides() {
        return Set.of(PROVIDES);
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        monitor = serviceExtensionContext.getMonitor();

        registerTransformers(serviceExtensionContext);

        monitor.info(String.format("Initialized %s", NAME));
    }

    private void registerTransformers(ServiceExtensionContext serviceExtensionContext) {
        var registry = serviceExtensionContext.getService(TransformerRegistry.class);

        Arrays.asList(
                new AssetToIdsArtifactTransformer(),
                new AssetToIdsRepresentationTransformer(),
                new AssetToIdsResourceTransformer(),
                new ConnectorToIdsConnectorTransformer(),
                new DataCatalogToIdsResourceCatalogTransformer(),
                new IdsIdToUriTransformer(),
                new IntegerToBigIntegerTransformer(),
                new SecurityProfileToSecurityProfileTransformer(),
                new StringToIdsMediaTypeTransformer(),
                new StringToUriTransformer(),
                new UriToIdsConnectorEndpointTransformer(),
                new UriToIdsIdTransformer(),
                new UriToIdsTypeTransformer()
        ).forEach(registry::register);
    }

    @Override
    public void start() {
        monitor.info(String.format("Started %s", NAME));
    }

    @Override
    public void shutdown() {
        monitor.info(String.format("Shutdown %s", NAME));
    }
}