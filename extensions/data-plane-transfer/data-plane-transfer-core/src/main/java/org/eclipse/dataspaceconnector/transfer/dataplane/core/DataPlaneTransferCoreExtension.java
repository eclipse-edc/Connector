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
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.core;

import org.eclipse.dataspaceconnector.common.token.TokenGenerationServiceImpl;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.transfer.dataplane.core.proxy.DataPlaneProxyManagerImpl;
import org.eclipse.dataspaceconnector.transfer.dataplane.core.proxy.DataPlaneProxyTokenGeneratorImpl;
import org.eclipse.dataspaceconnector.transfer.dataplane.core.security.NoopDataEncrypter;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyManager;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;

import java.security.PrivateKey;
import java.util.concurrent.TimeUnit;

@Provides({DataEncrypter.class, DataPlaneProxyManager.class})
public class DataPlaneTransferCoreExtension implements ServiceExtension {

    @EdcSetting
    private static final String DATA_PLANE_PUBLIC_API_ENDPOINT = "edc.transfer.dataplane.sync.endpoint";

    @EdcSetting
    private static final String DATA_PLANE_TOKEN_VALIDITY_SECONDS = "edc.transfer.dataplane.sync.token.validity";
    private static final long DEFAULT_DATA_PLANE_TOKEN_VALIDITY_SECONDS = TimeUnit.MINUTES.toSeconds(10);

    @EdcSetting
    private static final String TOKEN_SIGNER_PRIVATE_KEY_ALIAS = "edc.transfer.dataplane.token.signer.privatekey.alias";

    @Override
    public String name() {
        return "Data Plane Transfer Core";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var tokenValidity = context.getSetting(DATA_PLANE_TOKEN_VALIDITY_SECONDS, DEFAULT_DATA_PLANE_TOKEN_VALIDITY_SECONDS);
        var endpoint = context.getConfig().getString(DATA_PLANE_PUBLIC_API_ENDPOINT);

        var privateKeyAlias = context.getConfig().getString(TOKEN_SIGNER_PRIVATE_KEY_ALIAS);
        var privateKeyResolver = context.getService(PrivateKeyResolver.class);
        var privateKey = privateKeyResolver.resolvePrivateKey(privateKeyAlias, PrivateKey.class);
        var tokenGenerationService = new TokenGenerationServiceImpl(privateKey);

        var encrypter = new NoopDataEncrypter();
        context.registerService(DataEncrypter.class, encrypter);

        var dataPlaneProxyTokenGenerator = new DataPlaneProxyTokenGeneratorImpl(context.getTypeManager(), encrypter, tokenGenerationService, tokenValidity);
        var proxyManager = new DataPlaneProxyManagerImpl(endpoint, dataPlaneProxyTokenGenerator);
        context.registerService(DataPlaneProxyManager.class, proxyManager);
        context.registerService(EndpointDataReferenceTransformer.class, proxyManager);
    }
}
