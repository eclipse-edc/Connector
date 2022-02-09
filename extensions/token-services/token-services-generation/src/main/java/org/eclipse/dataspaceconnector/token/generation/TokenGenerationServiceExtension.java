package org.eclipse.dataspaceconnector.token.generation;


import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.token.generation.impl.TokenGenerationServiceImpl;
import org.eclipse.dataspaceconnector.token.spi.TokenGenerationService;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;

@Provides(TokenGenerationService.class)
public class TokenGenerationServiceExtension implements ServiceExtension {

    @EdcSetting
    private static final String PRIVATE_KEY_ALIAS = "edc.security.private-key.alias";

    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Override
    public String name() {
        return "Token Generation Service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var privateKeyAlias = context.getSetting(PRIVATE_KEY_ALIAS, null);
        if (privateKeyAlias == null) {
            throw new EdcException(String.format("Missing mandatory private key alias setting `%s`", PRIVATE_KEY_ALIAS));
        }

        var signer = createSigner(privateKeyAlias);
        var tokenGenerationService = new TokenGenerationServiceImpl(signer);
        context.registerService(TokenGenerationService.class, tokenGenerationService);
    }

    private JWSSigner createSigner(String pkAlias) {
        var privateKey = privateKeyResolver.resolvePrivateKey(pkAlias, PrivateKey.class);
        if (privateKey == null) {
            throw new EdcException("Failed to resolve private with alias: " + pkAlias);
        }

        if ("EC".equals(privateKey.getAlgorithm())) {
            try {
                return new ECDSASigner((ECPrivateKey) privateKey);
            } catch (JOSEException e) {
                throw new EdcException("Failed to load JWSSigner for EC private key: " + e);
            }
        } else {
            return new RSASSASigner(privateKey);
        }
    }
}
