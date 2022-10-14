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

package org.eclipse.dataspaceconnector.transfer.dataplane.sync;

import org.eclipse.dataspaceconnector.core.jwt.TokenGenerationServiceImpl;
import org.eclipse.dataspaceconnector.core.jwt.TokenValidationRulesRegistryImpl;
import org.eclipse.dataspaceconnector.core.jwt.TokenValidationServiceImpl;
import org.eclipse.dataspaceconnector.dataplane.selector.client.DataPlaneSelectorClient;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Setting;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyReferenceService;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.DataPlaneTokenValidationApiController;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.flow.ProviderDataPlaneProxyDataFlowController;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.proxy.DataPlaneTransferConsumerProxyTransformer;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.proxy.DataPlaneTransferProxyReferenceServiceImpl;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.proxy.DataPlaneTransferProxyResolverImpl;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.security.PublicKeyParser;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.validation.ContractValidationRule;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.validation.ExpirationDateValidationRule;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfig.DATA_PROXY_TOKEN_VALIDITY_SECONDS;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfig.DEFAULT_DATA_PROXY_TOKEN_VALIDITY_SECONDS;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfig.TOKEN_SIGNER_PRIVATE_KEY_ALIAS;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfig.TOKEN_VERIFIER_PUBLIC_KEY_ALIAS;

@Extension(value = DataPlaneTransferSyncExtension.NAME)
public class DataPlaneTransferSyncExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Transfer Sync";
    @Setting
    private static final String DPF_SELECTOR_STRATEGY = "edc.transfer.client.selector.strategy";
    private static final String API_CONTEXT_ALIAS = "validation";
    @Inject
    private DataPlaneSelectorClient selectorClient;

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Inject
    private WebService webService;

    @Inject
    private DataFlowManager dataFlowManager;

    @Inject
    private EndpointDataReferenceTransformerRegistry transformerRegistry;

    @Inject
    private Vault vault;

    @Inject
    private Clock clock;

    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Inject
    private DataEncrypter dataEncrypter;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var keyPair = createKeyPair(context);
        var selectorStrategy = context.getSetting(DPF_SELECTOR_STRATEGY, "random");

        var proxyResolver = new DataPlaneTransferProxyResolverImpl(selectorClient, selectorStrategy);

        var controller = createTokenValidationApiController(keyPair.getPublic(), dataEncrypter, context.getTypeManager());
        webService.registerResource(API_CONTEXT_ALIAS, controller);

        var proxyReferenceService = createProxyReferenceService(context, keyPair.getPrivate(), dataEncrypter);
        var flowController = new ProviderDataPlaneProxyDataFlowController(context.getConnectorId(), proxyResolver, dispatcherRegistry, proxyReferenceService);
        dataFlowManager.register(flowController);

        var consumerProxyTransformer = new DataPlaneTransferConsumerProxyTransformer(proxyResolver, proxyReferenceService);
        transformerRegistry.registerTransformer(consumerProxyTransformer);
    }

    /**
     * Creates service generating {@link org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference} corresponding
     * to a http proxy.
     */
    private DataPlaneTransferProxyReferenceService createProxyReferenceService(ServiceExtensionContext context, PrivateKey privateKey, DataEncrypter encrypter) {
        var tokenValiditySeconds = context.getSetting(DATA_PROXY_TOKEN_VALIDITY_SECONDS, DEFAULT_DATA_PROXY_TOKEN_VALIDITY_SECONDS);
        var tokenGenerationService = new TokenGenerationServiceImpl(privateKey);
        return new DataPlaneTransferProxyReferenceServiceImpl(tokenGenerationService, context.getTypeManager(), tokenValiditySeconds, encrypter, clock);
    }

    /**
     * Register the API controller that is used for validating tokens received in input of Data Plane API.
     */
    private DataPlaneTokenValidationApiController createTokenValidationApiController(PublicKey publicKey, DataEncrypter encrypter, TypeManager typeManager) {
        var registry = new TokenValidationRulesRegistryImpl();
        registry.addRule(new ContractValidationRule(contractNegotiationStore, clock));
        registry.addRule(new ExpirationDateValidationRule(clock));
        var tokenValidationService = new TokenValidationServiceImpl(id -> publicKey, registry);
        return new DataPlaneTokenValidationApiController(tokenValidationService, encrypter, typeManager);
    }

    /**
     * Build the private/public key pair used for signing/verifying token generated by this extension.
     */
    private KeyPair createKeyPair(ServiceExtensionContext context) {
        var config = context.getConfig();

        var privateKeyAlias = config.getString(TOKEN_SIGNER_PRIVATE_KEY_ALIAS);
        var privateKey = privateKeyResolver.resolvePrivateKey(privateKeyAlias, PrivateKey.class);
        Objects.requireNonNull(privateKey, "Failed to resolve private key with alias: " + privateKeyAlias);

        var publicKeyAlias = config.getString(TOKEN_VERIFIER_PUBLIC_KEY_ALIAS);
        var publicKeyPem = vault.resolveSecret(publicKeyAlias);
        Objects.requireNonNull(publicKeyPem, "Failed to resolve public key secret with alias: " + publicKeyPem);
        var publicKey = PublicKeyParser.from(publicKeyPem);
        return new KeyPair(publicKey, privateKey);
    }
}
