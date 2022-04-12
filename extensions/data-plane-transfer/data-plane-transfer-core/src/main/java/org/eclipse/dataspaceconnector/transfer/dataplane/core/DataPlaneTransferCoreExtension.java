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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultPrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.transfer.dataplane.core.proxy.DataPlaneProxyAccessManagerImpl;
import org.eclipse.dataspaceconnector.transfer.dataplane.core.security.NoopDataEncrypter;
import org.eclipse.dataspaceconnector.transfer.dataplane.core.token.DataPlaneTransferTokenGeneratorImpl;
import org.eclipse.dataspaceconnector.transfer.dataplane.core.token.DataPlaneTransferTokenValidatorImpl;
import org.eclipse.dataspaceconnector.transfer.dataplane.core.token.DataPlaneTransferValidationRulesRegistryImpl;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyAccessManager;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.token.DataPlaneTransferTokenGenerator;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.token.DataPlaneTransferTokenValidator;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.token.DataPlaneTransferValidationRulesRegistry;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Provides({DataEncrypter.class, DataPlaneProxyAccessManager.class, DataPlaneTransferTokenGenerator.class,
        DataPlaneTransferTokenValidator.class, DataPlaneTransferValidationRulesRegistry.class})
public class DataPlaneTransferCoreExtension implements ServiceExtension {

    @EdcSetting
    private static final String DATA_PROXY_ENDPOINT = "edc.transfer.proxy.endpoint";

    @EdcSetting
    private static final String DATA_PROXY_TOKEN_VALIDITY_SECONDS = "edc.transfer.proxy.token.validity.seconds";
    private static final long DEFAULT_DATA_PROXY_TOKEN_VALIDITY_SECONDS = TimeUnit.MINUTES.toSeconds(10);

    @EdcSetting
    private static final String TOKEN_SIGNER_PRIVATE_KEY_ALIAS = "edc.transfer.proxy.token.signer.privatekey.alias";

    @EdcSetting
    private static final String TOKEN_VERIFIER_PUBLIC_KEY_ALIAS = "edc.transfer.proxy.token.verifier.publickey.alias";

    @Override
    public String name() {
        return "Data Plane Transfer Core";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);
        var resolver = createResolver(vault);

        var privateKeyAlias = context.getConfig().getString(TOKEN_SIGNER_PRIVATE_KEY_ALIAS);
        var publicKeyAlias = context.getSetting(TOKEN_VERIFIER_PUBLIC_KEY_ALIAS, privateKeyAlias + "-pub");
        var tokenValidity = context.getSetting(DATA_PROXY_TOKEN_VALIDITY_SECONDS, DEFAULT_DATA_PROXY_TOKEN_VALIDITY_SECONDS);
        var endpoint = context.getConfig().getString(DATA_PROXY_ENDPOINT);

        var validationRulesRegistry = new DataPlaneTransferValidationRulesRegistryImpl();
        context.registerService(DataPlaneTransferValidationRulesRegistry.class, validationRulesRegistry);

        var tokenGenerator = createTokenGenerator(resolver, privateKeyAlias);
        context.registerService(DataPlaneTransferTokenGenerator.class, tokenGenerator);

        var tokenValidator = createTokenValidator(resolver, publicKeyAlias, validationRulesRegistry);
        context.registerService(DataPlaneTransferTokenValidator.class, tokenValidator);

        var encrypter = new NoopDataEncrypter();
        context.registerService(DataEncrypter.class, encrypter);

        var proxyManager = new DataPlaneProxyAccessManagerImpl(endpoint, tokenGenerator, context.getTypeManager(), encrypter, tokenValidity);
        context.registerService(DataPlaneProxyAccessManager.class, proxyManager);
        context.registerService(EndpointDataReferenceTransformer.class, proxyManager);
    }

    /**
     * Creates a {@link DataPlaneTransferTokenGenerator} used to generate tokens provided in input of Data Plane public API.
     */
    private DataPlaneTransferTokenGenerator createTokenGenerator(PrivateKeyResolver resolver, String privateKeyAlias) {
        try {
            var privateKey = resolveMandatoryKey(resolver, privateKeyAlias).toPrivateKey();
            return new DataPlaneTransferTokenGeneratorImpl(privateKey);
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }

    /**
     * Creates {@link DataPlaneTransferTokenValidator} used to verify tokens provided in input of Data Plane public API.
     * Note that the public key used to validate the token is only used by the current Control Plane, so it is stored into
     * the same secret store as the associated private key. This is why {@link PrivateKeyResolver} is also used to access the
     * public key.
     */
    private DataPlaneTransferTokenValidator createTokenValidator(PrivateKeyResolver resolver, String publicKeyAlias, DataPlaneTransferValidationRulesRegistry validationRulesRegistry) {
        try {
            var publicKey = resolveMandatoryKey(resolver, publicKeyAlias).toPublicKey();
            return new DataPlaneTransferTokenValidatorImpl(id -> publicKey, validationRulesRegistry);
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }

    private AsymmetricJWK resolveMandatoryKey(PrivateKeyResolver resolver, String id) {
        return Optional.ofNullable(resolver.resolvePrivateKey(id, AsymmetricJWK.class))
                .orElseThrow(() -> new EdcException("Failed to resolve key: " + id));
    }

    /**
     * Registers {@link org.eclipse.dataspaceconnector.spi.security.KeyParser} for supporting EC/RSA keys.
     */
    private PrivateKeyResolver createResolver(Vault vault) {
        var resolver = new VaultPrivateKeyResolver(vault);
        resolver.addParser(AsymmetricJWK.class, encoded -> {
            try {
                return (AsymmetricJWK) JWK.parseFromPEMEncodedObjects(encoded);
            } catch (JOSEException e) {
                throw new EdcException(e);
            }
        });
        return resolver;
    }
}
