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

package org.eclipse.edc.connector.dataplane.transfer;

import org.eclipse.edc.connector.api.control.configuration.ControlApiConfiguration;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.dataplane.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.transfer.api.ConsumerPullTransferTokenValidationApiController;
import org.eclipse.edc.connector.dataplane.transfer.flow.ConsumerPullTransferDataFlowController;
import org.eclipse.edc.connector.dataplane.transfer.flow.ProviderPushTransferDataFlowController;
import org.eclipse.edc.connector.dataplane.transfer.proxy.ConsumerPullTransferEndpointDataReferenceServiceImpl;
import org.eclipse.edc.connector.dataplane.transfer.proxy.ConsumerPullTransferProxyResolver;
import org.eclipse.edc.connector.dataplane.transfer.proxy.ConsumerPullTransferProxyTransformer;
import org.eclipse.edc.connector.dataplane.transfer.spi.proxy.ConsumerPullTransferEndpointDataReferenceService;
import org.eclipse.edc.connector.dataplane.transfer.spi.security.DataEncrypter;
import org.eclipse.edc.connector.dataplane.transfer.spi.security.KeyPairWrapper;
import org.eclipse.edc.connector.dataplane.transfer.validation.ContractValidationRule;
import org.eclipse.edc.connector.dataplane.transfer.validation.ExpirationDateValidationRule;
import org.eclipse.edc.connector.transfer.spi.callback.ControlPlaneApiUrl;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.jwt.TokenGenerationServiceImpl;
import org.eclipse.edc.jwt.TokenValidationRulesRegistryImpl;
import org.eclipse.edc.jwt.TokenValidationServiceImpl;
import org.eclipse.edc.jwt.spi.TokenValidationService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.web.spi.WebService;

import java.time.Clock;

import static java.lang.String.format;
import static org.eclipse.edc.connector.dataplane.transfer.DataPlaneTransferConfig.DEFAULT_TOKEN_VALIDITY_SECONDS;
import static org.eclipse.edc.connector.dataplane.transfer.DataPlaneTransferConfig.TOKEN_VALIDITY_SECONDS;

@Extension(value = DataPlaneTransferCoreExtension.NAME)
public class DataPlaneTransferCoreExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Transfer Core";

    /**
     * This deprecation is used to permit a softer transition from the deprecated `web.http.validation` config group to
     * the current `web.http.control`
     *
     * @deprecated "web.http.control" config should be used instead of "web.http.validation"
     */
    @Deprecated(since = "milestone8")
    private static final String DEPRECATED_API_CONTEXT_ALIAS = "validation";

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
    private Clock clock;

    @Inject
    private DataEncrypter dataEncrypter;

    @Inject
    private DataPlaneClient dataPlaneClient;

    @Inject
    private ControlApiConfiguration controlApiConfiguration;

    @Inject
    private KeyPairWrapper keyPairWrapper;

    @Inject
    private ConsumerPullTransferProxyResolver proxyResolver;

    @Inject(required = false)
    private ControlPlaneApiUrl callbackUrl;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var tokenValidationService = createTokenValidationService();
        webService.registerResource(getApiContext(context), new ConsumerPullTransferTokenValidationApiController(tokenValidationService, dataEncrypter, context.getTypeManager()));

        var proxyReferenceService = createDataProxyReferenceService(context.getConfig(), context.getTypeManager());
        dataFlowManager.register(new ConsumerPullTransferDataFlowController(context.getConnectorId(), proxyResolver, proxyReferenceService, dispatcherRegistry));
        dataFlowManager.register(new ProviderPushTransferDataFlowController(callbackUrl, dataPlaneClient));

        var consumerProxyTransformer = new ConsumerPullTransferProxyTransformer(proxyResolver, proxyReferenceService);
        transformerRegistry.registerTransformer(consumerProxyTransformer);
    }

    /**
     * Determines on which context the token validation API controller should be registered. The `validation` context
     * is still maintained for backward compatibility purpose but standard context should come from the {@link ControlApiConfiguration}.
     */
    private String getApiContext(ServiceExtensionContext context) {
        if (context.getConfig().hasPath("web.http." + DEPRECATED_API_CONTEXT_ALIAS)) {
            context.getMonitor().warning(
                    format("Deprecated settings group %s is being used for Control API configuration, please switch to the new group %s",
                            "web.http." + DEPRECATED_API_CONTEXT_ALIAS, "web.http.control"));
            return DEPRECATED_API_CONTEXT_ALIAS;
        }
        return controlApiConfiguration.getContextAlias();
    }

    /**
     * Creates service generating {@link EndpointDataReference} corresponding
     * to a http proxy.
     */
    private ConsumerPullTransferEndpointDataReferenceService createDataProxyReferenceService(Config config, TypeManager typeManager) {
        var tokenValiditySeconds = config.getLong(TOKEN_VALIDITY_SECONDS, DEFAULT_TOKEN_VALIDITY_SECONDS);
        var tokenGenerationService = new TokenGenerationServiceImpl(keyPairWrapper.get().getPrivate());
        return new ConsumerPullTransferEndpointDataReferenceServiceImpl(tokenGenerationService, typeManager, tokenValiditySeconds, dataEncrypter, clock);
    }

    /**
     * Service in charge of validating access token sent by the Data Plane.
     */
    private TokenValidationService createTokenValidationService() {
        var registry = new TokenValidationRulesRegistryImpl();
        registry.addRule(new ContractValidationRule(contractNegotiationStore, clock));
        registry.addRule(new ExpirationDateValidationRule(clock));
        return new TokenValidationServiceImpl(id -> keyPairWrapper.get().getPublic(), registry);
    }
}
