/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.atlas.metadata;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.schema.SchemaRegistry;
import org.eclipse.dataspaceconnector.spi.metadata.MetadataObservable;
import org.eclipse.dataspaceconnector.spi.metadata.MetadataStore;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class AtlasExtension implements ServiceExtension {

    public static final String ATLAS_FEATURE = "atlas";
    private static final String SECRET_ATLAS_USER = "atlas-username";
    private static final String SECRET_ATLAS_PWD = "atlas-password";
    private static final String DEFAULT_ATLAS_URL = "http://localhost:21000";
    private static final String ATLAS_URL_PROPERTY = "edc.atlas.url";

    @Override
    public Set<String> provides() {
        return Set.of(AtlasExtension.ATLAS_FEATURE, "dataspaceconnector:metadata-store-observable");
    }

    @Override
    public Set<String> requires() {
        return Set.of("dataspaceconnector:http-client", SchemaRegistry.FEATURE);
    }

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);

        var atlasUrl = context.getSetting(AtlasExtension.ATLAS_URL_PROPERTY, AtlasExtension.DEFAULT_ATLAS_URL);

        var user = vault.resolveSecret(AtlasExtension.SECRET_ATLAS_USER);
        var pwd = vault.resolveSecret(AtlasExtension.SECRET_ATLAS_PWD);
        var api = new AtlasApiImpl(atlasUrl, user, pwd, context.getService(OkHttpClient.class), context.getTypeManager());
        context.registerService(AtlasApi.class, api);
        AtlasMetadataStore store = new AtlasMetadataStore(api, context.getMonitor(), context.getService(SchemaRegistry.class));
        context.registerService(MetadataStore.class, store);
        context.registerService(MetadataObservable.class, store);

        context.getMonitor().info("Initialized Atlas API extension.");

        context.getTypeManager().registerTypes(AtlasDataCatalogEntry.class);
    }


}
