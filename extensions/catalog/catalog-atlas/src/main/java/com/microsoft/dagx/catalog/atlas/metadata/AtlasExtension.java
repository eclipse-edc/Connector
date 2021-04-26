package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import org.apache.atlas.AtlasClientV2;

import java.util.Set;

public class AtlasExtension implements ServiceExtension {

    private static final String SECRET_ATLAS_USER = "atlas-username";
    private static final String SECRET_ATLAS_PWD = "atlas-password";
    public static final String ATLAS_FEATURE = "atlas";

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
        var api = new AtlasApiImpl(createAtlasClient(vault));
        context.registerService(AtlasApi.class, api);
        context.getMonitor().info("Initialized Atlas API extension.");
    }

    private AtlasClientV2 createAtlasClient(Vault vault) {

        var user = vault.resolveSecret(SECRET_ATLAS_USER);
        var pwd = vault.resolveSecret(SECRET_ATLAS_PWD);
        return new AtlasClientV2(new String[]{"http://localhost:21000"}, new String[]{user, pwd});
    }

}
