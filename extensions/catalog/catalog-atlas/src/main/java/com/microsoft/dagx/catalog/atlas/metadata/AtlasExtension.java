/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.schema.SchemaRegistry;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import okhttp3.OkHttpClient;

import java.util.Set;

public class AtlasExtension implements ServiceExtension {

    public static final String ATLAS_FEATURE = "atlas";
    private static final String SECRET_ATLAS_USER = "atlas-username";
    private static final String SECRET_ATLAS_PWD = "atlas-password";
    private static final String DEFAULT_ATLAS_URL = "http://localhost:21000";
    private static final String ATLAS_URL_PROPERTY = "dagx.atlas.url";

    @Override
    public Set<String> provides() {
        return Set.of(ATLAS_FEATURE);
    }

    @Override
    public Set<String> requires() {
        return Set.of("dagx:http-client", SchemaRegistry.FEATURE);
    }

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);

        var atlasUrl = context.getSetting(ATLAS_URL_PROPERTY, DEFAULT_ATLAS_URL);

        var user = vault.resolveSecret(SECRET_ATLAS_USER);
        var pwd = vault.resolveSecret(SECRET_ATLAS_PWD);
        var api = new AtlasApiImpl(atlasUrl, user, pwd, context.getService(OkHttpClient.class), context.getTypeManager());
        context.registerService(AtlasApi.class, api);
        context.registerService(MetadataStore.class, new AtlasMetadataStore(api, context.getMonitor(), context.getService(SchemaRegistry.class)));
        context.getMonitor().info("Initialized Atlas API extension.");

        context.getTypeManager().registerTypes(AtlasDataCatalogEntry.class);
    }


}
