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

package org.eclipse.edc.connector.transfer.dataplane;

import org.eclipse.edc.connector.api.control.configuration.ControlApiConfiguration;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.dataplane.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.transfer.dataplane.api.ConsumerPullTransferTokenValidationApiController;
import org.eclipse.edc.connector.transfer.dataplane.flow.ConsumerPullTransferDataFlowController;
import org.eclipse.edc.connector.transfer.dataplane.flow.ProviderPushTransferDataFlowController;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullTransferEndpointDataReferenceServiceImpl;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullTransferProxyResolver;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullTransferProxyTransformer;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullTransferEndpointDataReferenceService;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.KeyPairWrapper;
import org.eclipse.edc.connector.transfer.dataplane.validation.ContractValidationRule;
import org.eclipse.edc.connector.transfer.dataplane.validation.ExpirationDateValidationRule;
import org.eclipse.edc.connector.transfer.spi.callback.ControlPlaneApiUrl;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.jwt.TokenGenerationServiceImpl;
import org.eclipse.edc.jwt.TokenValidationRulesRegistryImpl;
import org.eclipse.edc.jwt.TokenValidationServiceImpl;
import org.eclipse.edc.jwt.spi.TokenValidationService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.web.spi.WebService;

import java.time.Clock;

import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.DEFAULT_TOKEN_VALIDITY_SECONDS;
import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.TOKEN_VALIDITY_SECONDS;

@Extension(value = TransferDataPlaneCoreExtension.NAME)
public class TransferDataPlaneCoreExtension implements ServiceExtension {

    public static final String NAME = "Transfer Data Plane Core";

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

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

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var tokenValidationService = createTokenValidationService();
        webService.registerResource(controlApiConfiguration.getContextAlias(), new ConsumerPullTransferTokenValidationApiController(tokenValidationService, dataEncrypter, typeManager));

        var proxyReferenceService = createDataProxyReferenceService(context.getConfig(), typeManager);
        dataFlowManager.register(new ConsumerPullTransferDataFlowController(proxyResolver, proxyReferenceService));
        dataFlowManager.register(new ProviderPushTransferDataFlowController(callbackUrl, dataPlaneClient));

        var consumerProxyTransformer = new ConsumerPullTransferProxyTransformer(proxyResolver, proxyReferenceService);
        transformerRegistry.registerTransformer(consumerProxyTransformer);
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
