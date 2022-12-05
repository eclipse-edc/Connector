/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *       Mercedes-Benz Tech Innovation GmbH - DataEncrypter can be provided by extensions
 *
 */

package org.eclipse.edc.connector.transfer.dataplane;

import org.eclipse.edc.connector.dataplane.client.EmbeddedDataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.spi.DataPlanePublicApiUrl;
import org.eclipse.edc.connector.dataplane.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullTransferEmbeddedProxyResolver;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullTransferProxyResolver;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullTransferRemoteProxyResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.DEFAULT_DPF_SELECTOR_STRATEGY;
import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.DPF_SELECTOR_STRATEGY;

@Extension(value = ConsumerPullTransferProxyResolverExtension.NAME)
public class ConsumerPullTransferProxyResolverExtension implements ServiceExtension {

    public static final String NAME = "Consumer Pull Transfer Proxy Resolver";

    @Inject
    private DataPlaneSelectorClient selectorClient;

    @Inject
    private DataPlaneClient dataPlaneClient;

    @Inject(required = false)
    private DataPlanePublicApiUrl dataPlanePublicApiUrl;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public ConsumerPullTransferProxyResolver proxyResolver(ServiceExtensionContext context) {
        // If it's embedded DataPlane and it provides the APIs use the embedded DataPlane public URL
        if (dataPlaneClient instanceof EmbeddedDataPlaneClient && dataPlanePublicApiUrl != null) {
            context.getMonitor().info("Using embedded proxy resolver for 'consumer pull' transfer");
            return new ConsumerPullTransferEmbeddedProxyResolver(dataPlanePublicApiUrl);
        }

        var selectorStrategy = context.getSetting(DPF_SELECTOR_STRATEGY, DEFAULT_DPF_SELECTOR_STRATEGY);
        return new ConsumerPullTransferRemoteProxyResolver(selectorClient, selectorStrategy);
    }
}
