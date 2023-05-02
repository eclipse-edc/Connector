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

import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullTransferProxyResolver;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullTransferProxyResolverImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.DEFAULT_DPF_SELECTOR_STRATEGY;
import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.DPF_SELECTOR_STRATEGY;

@Extension(value = ConsumerPullTransferProxyResolverExtension.NAME)
@Provides(ConsumerPullTransferProxyResolver.class)
public class ConsumerPullTransferProxyResolverExtension implements ServiceExtension {

    public static final String NAME = "Consumer Pull Transfer Proxy Resolver";

    @Inject
    private DataPlaneSelectorClient selectorClient;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public ConsumerPullTransferProxyResolver proxyResolver(ServiceExtensionContext context) {
        var selectorStrategy = context.getSetting(DPF_SELECTOR_STRATEGY, DEFAULT_DPF_SELECTOR_STRATEGY);
        return new ConsumerPullTransferProxyResolverImpl(selectorClient, selectorStrategy);
    }
}
