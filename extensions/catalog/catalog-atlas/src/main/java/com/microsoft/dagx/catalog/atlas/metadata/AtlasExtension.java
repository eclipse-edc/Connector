package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import org.apache.atlas.AtlasClientV2;

import java.util.Set;

public class AtlasExtension implements ServiceExtension {

    public static final String ATLAS_FEATURE = "atlas";
    private static final String SECRET_ATLAS_USER = "atlas-username";
    private static final String SECRET_ATLAS_PWD = "atlas-password";
    private static final String DEFAULT_ATLAS_URL = "http://localhost:21000";
    private static final String ATLAS_URL_PROPERTY = "atlas.url";

    @Override
    public Set<String> provides() {
        return Set.of(ATLAS_FEATURE);
    }


    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);

        var atlasUrl = context.getSetting(ATLAS_URL_PROPERTY, DEFAULT_ATLAS_URL);


        var api = new AtlasApiImpl(createAtlasClient(vault, atlasUrl));
        context.registerService(AtlasApi.class, api);
        context.getMonitor().info("Initialized Atlas API extension.");
    }

    private AtlasClientV2 createAtlasClient(Vault vault, String atlasUrl) {

        var user = vault.resolveSecret(SECRET_ATLAS_USER);
        var pwd = vault.resolveSecret(SECRET_ATLAS_PWD);

        return new AtlasClientV2(new String[]{atlasUrl}, new String[]{user, pwd});
    }

}
