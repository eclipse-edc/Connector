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

package org.eclipse.edc.connector.dataplane.transfer.sync;

import org.eclipse.edc.connector.api.control.configuration.ControlApiConfiguration;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.transfer.spi.proxy.DataPlaneTransferProxyReferenceService;
import org.eclipse.edc.connector.dataplane.transfer.spi.security.DataEncrypter;
import org.eclipse.edc.connector.dataplane.transfer.sync.api.DataPlaneTokenValidationApiController;
import org.eclipse.edc.connector.dataplane.transfer.sync.flow.ProviderDataPlaneProxyDataFlowController;
import org.eclipse.edc.connector.dataplane.transfer.sync.proxy.DataPlaneTransferConsumerProxyTransformer;
import org.eclipse.edc.connector.dataplane.transfer.sync.proxy.DataPlaneTransferProxyReferenceServiceImpl;
import org.eclipse.edc.connector.dataplane.transfer.sync.proxy.DataPlaneTransferProxyResolverImpl;
import org.eclipse.edc.connector.dataplane.transfer.sync.security.PublicKeyParser;
import org.eclipse.edc.connector.dataplane.transfer.sync.validation.ContractValidationRule;
import org.eclipse.edc.connector.dataplane.transfer.sync.validation.ExpirationDateValidationRule;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.jwt.TokenGenerationServiceImpl;
import org.eclipse.edc.jwt.TokenValidationRulesRegistryImpl;
import org.eclipse.edc.jwt.TokenValidationServiceImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.web.spi.WebService;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;
import java.util.Objects;

import static java.lang.String.format;

@Extension(value = DataPlaneTransferSyncExtension.NAME)
public class DataPlaneTransferSyncExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Transfer Sync";
    @Setting
    private static final String DPF_SELECTOR_STRATEGY = "edc.transfer.client.selector.strategy";

    /**
     * This deprecation is used to permit a softer transition from the deprecated `web.http.validation` config group to
     * the current `web.http.control`
     *
     * @deprecated "web.http.control" config should be used instead of "web.http.validation"
     */
    @Deprecated(since = "milestone8")
    private static final String DEPRECATED_API_CONTEXT_ALIAS = "validation";

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

    @Inject
    private ControlApiConfiguration controlApiConfiguration;

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
        if (context.getConfig().hasPath("web.http." + DEPRECATED_API_CONTEXT_ALIAS)) {
            webService.registerResource(DEPRECATED_API_CONTEXT_ALIAS, controller);
            context.getMonitor().warning(
                    format("Deprecated settings group %s is being used for Control API configuration, please switch to the new group %s",
                            "web.http." + DEPRECATED_API_CONTEXT_ALIAS, "web.http.control"));
        } else {
            webService.registerResource(controlApiConfiguration.getContextAlias(), controller);
        }

        var proxyReferenceService = createProxyReferenceService(context, keyPair.getPrivate(), dataEncrypter);
        var flowController = new ProviderDataPlaneProxyDataFlowController(context.getConnectorId(), proxyResolver, dispatcherRegistry, proxyReferenceService);
        dataFlowManager.register(flowController);

        var consumerProxyTransformer = new DataPlaneTransferConsumerProxyTransformer(proxyResolver, proxyReferenceService);
        transformerRegistry.registerTransformer(consumerProxyTransformer);
    }

    /**
     * Creates service generating {@link EndpointDataReference} corresponding
     * to a http proxy.
     */
    private DataPlaneTransferProxyReferenceService createProxyReferenceService(ServiceExtensionContext context, PrivateKey privateKey, DataEncrypter encrypter) {
        var tokenValiditySeconds = context.getSetting(DataPlaneTransferSyncConfig.DATA_PROXY_TOKEN_VALIDITY_SECONDS, DataPlaneTransferSyncConfig.DEFAULT_DATA_PROXY_TOKEN_VALIDITY_SECONDS);
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

        var privateKeyAlias = config.getString(DataPlaneTransferSyncConfig.TOKEN_SIGNER_PRIVATE_KEY_ALIAS);
        var privateKey = privateKeyResolver.resolvePrivateKey(privateKeyAlias, PrivateKey.class);
        Objects.requireNonNull(privateKey, "Failed to resolve private key with alias: " + privateKeyAlias);

        var publicKeyAlias = config.getString(DataPlaneTransferSyncConfig.TOKEN_VERIFIER_PUBLIC_KEY_ALIAS);
        var publicKeyPem = vault.resolveSecret(publicKeyAlias);
        Objects.requireNonNull(publicKeyPem, "Failed to resolve public key secret with alias: " + publicKeyPem);
        var publicKey = PublicKeyParser.from(publicKeyPem);
        return new KeyPair(publicKey, privateKey);
    }
}
