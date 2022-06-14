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

package org.eclipse.dataspaceconnector.transfer.dataplane.sync;

import org.eclipse.dataspaceconnector.common.token.TokenGenerationServiceImpl;
import org.eclipse.dataspaceconnector.common.token.TokenValidationRulesRegistryImpl;
import org.eclipse.dataspaceconnector.common.token.TokenValidationServiceImpl;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyReferenceService;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.DataPlaneTransferTokenValidationApiController;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.flow.ProviderDataPlaneProxyDataFlowController;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.proxy.DataPlaneTransferConsumerProxyTransformer;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.proxy.DataPlaneTransferProxyReferenceServiceImpl;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.security.NoopDataEncrypter;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.security.PublicKeyParser;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.validation.ContractValidationRule;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.validation.ExpirationDateValidationRule;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfig.DATA_PROXY_ENDPOINT;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfig.DATA_PROXY_TOKEN_VALIDITY_SECONDS;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfig.DEFAULT_DATA_PROXY_TOKEN_VALIDITY_SECONDS;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfig.TOKEN_SIGNER_PRIVATE_KEY_ALIAS;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.DataPlaneTransferSyncConfig.TOKEN_VERIFIER_PUBLIC_KEY_ALIAS;

public class DataPlaneTransferSyncExtension implements ServiceExtension {

    private static final String API_CONTEXT_ALIAS = "validation";

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

    @Override
    public String name() {
        return "Data Plane Transfer Sync";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var proxyEndpoint = context.getConfig().getString(DATA_PROXY_ENDPOINT); // TODO: this should determined dynamically for every DataRequest through DataPlaneSelector

        var keyPair = createKeyPair(context);
        var encrypter = new NoopDataEncrypter();

        var controller = createTokenValidationApiController(context.getMonitor(), keyPair.getPublic(), encrypter);
        webService.registerResource(API_CONTEXT_ALIAS, controller);

        var proxyReferenceService = createProxyReferenceService(context, keyPair.getPrivate(), encrypter);
        var flowController = new ProviderDataPlaneProxyDataFlowController(context.getConnectorId(), proxyEndpoint, dispatcherRegistry, proxyReferenceService);
        dataFlowManager.register(flowController);

        var consumerProxyTransformer = new DataPlaneTransferConsumerProxyTransformer(proxyEndpoint, proxyReferenceService);
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
    private DataPlaneTransferTokenValidationApiController createTokenValidationApiController(Monitor monitor, PublicKey publicKey, DataEncrypter encrypter) {
        var registry = new TokenValidationRulesRegistryImpl();
        registry.addRule(new ContractValidationRule(contractNegotiationStore, clock));
        registry.addRule(new ExpirationDateValidationRule(clock));
        var tokenValidationService = new TokenValidationServiceImpl(id -> publicKey, registry);
        return new DataPlaneTransferTokenValidationApiController(monitor, tokenValidationService, encrypter);
    }

    /**
     * Build the private/public key pair used for signing/verifying token generated by this extension.
     */
    private KeyPair createKeyPair(ServiceExtensionContext context) {
        var config = context.getConfig();

        var privateKeyAlias = config.getString(TOKEN_SIGNER_PRIVATE_KEY_ALIAS);
        var privateKey = privateKeyResolver.resolvePrivateKey(privateKeyAlias, PrivateKey.class);
        Objects.requireNonNull(privateKey, "Failed to resolve private key with alias: " + privateKeyAlias);

        var publicKeyAlias = config.getString(TOKEN_VERIFIER_PUBLIC_KEY_ALIAS, privateKeyAlias + "-pub");
        var publicKeyPem = vault.resolveSecret(publicKeyAlias);
        Objects.requireNonNull(publicKeyPem, "Failed to resolve public key secret with alias: " + publicKeyPem);
        var publicKey = PublicKeyParser.from(publicKeyPem);
        return new KeyPair(publicKey, privateKey);
    }
}
