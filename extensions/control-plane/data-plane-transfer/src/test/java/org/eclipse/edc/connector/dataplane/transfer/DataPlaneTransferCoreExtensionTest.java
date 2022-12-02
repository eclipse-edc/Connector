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
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.connector.dataplane.transfer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.eclipse.edc.connector.api.control.configuration.ControlApiConfiguration;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.dataplane.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.transfer.api.ConsumerPullTransferTokenValidationApiController;
import org.eclipse.edc.connector.dataplane.transfer.flow.ConsumerPullTransferDataFlowController;
import org.eclipse.edc.connector.dataplane.transfer.flow.ProviderPushTransferDataFlowController;
import org.eclipse.edc.connector.dataplane.transfer.proxy.ConsumerPullTransferProxyResolver;
import org.eclipse.edc.connector.dataplane.transfer.proxy.ConsumerPullTransferProxyTransformer;
import org.eclipse.edc.connector.dataplane.transfer.spi.security.DataEncrypter;
import org.eclipse.edc.connector.dataplane.transfer.spi.security.KeyPairWrapper;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.KeyPair;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneTransferCoreExtensionTest {

    private static final String CONTROL_PLANE_API_CONTEXT = "control";

    private ServiceExtensionContext context;
    private WebService webServiceMock;
    private EndpointDataReferenceTransformerRegistry endpointDataReferenceTransformerRegistryMock;
    private DataFlowManager dataFlowManagerMock;
    private DataPlaneTransferCoreExtension extension;

    @BeforeEach
    public void setUp(ServiceExtensionContext context, ObjectFactory factory) throws JOSEException {
        var monitor = mock(Monitor.class);
        this.webServiceMock = mock(WebService.class);
        ControlApiConfiguration controlApiConfigurationMock = mock(ControlApiConfiguration.class);
        when(controlApiConfigurationMock.getContextAlias()).thenReturn(CONTROL_PLANE_API_CONTEXT);
        this.endpointDataReferenceTransformerRegistryMock = mock(EndpointDataReferenceTransformerRegistry.class);
        this.dataFlowManagerMock = mock(DataFlowManager.class);

        context.registerService(PrivateKeyResolver.class, mock(PrivateKeyResolver.class));
        context.registerService(Vault.class, mock(Vault.class));
        context.registerService(WebService.class, webServiceMock);
        context.registerService(ContractNegotiationStore.class, mock(ContractNegotiationStore.class));
        context.registerService(RemoteMessageDispatcherRegistry.class, mock(RemoteMessageDispatcherRegistry.class));
        context.registerService(DataFlowManager.class, dataFlowManagerMock);
        context.registerService(DataEncrypter.class, mock(DataEncrypter.class));
        context.registerService(ControlApiConfiguration.class, controlApiConfigurationMock);
        var keyPair = generateRandomKeyPair();
        var keyPairWrapper = mock(KeyPairWrapper.class);
        when(keyPairWrapper.get()).thenReturn(keyPair);
        context.registerService(KeyPairWrapper.class, keyPairWrapper);
        context.registerService(EndpointDataReferenceTransformerRegistry.class, endpointDataReferenceTransformerRegistryMock);
        context.registerService(DataPlaneClient.class, mock(DataPlaneClient.class));
        context.registerService(ConsumerPullTransferProxyResolver.class, mock(ConsumerPullTransferProxyResolver.class));

        this.context = spy(context); //used to inject the config
        when(this.context.getMonitor()).thenReturn(monitor);

        extension = factory.constructInstance(DataPlaneTransferCoreExtension.class);
    }

    @Test
    void verifyRegisterBaseServices() {
        extension.initialize(context);

        verify(dataFlowManagerMock).register(any(ConsumerPullTransferDataFlowController.class));
        verify(dataFlowManagerMock).register(any(ProviderPushTransferDataFlowController.class));
        verify(endpointDataReferenceTransformerRegistryMock).registerTransformer(any(ConsumerPullTransferProxyTransformer.class));
    }

    @Test
    void verifyControllerRegisteredOnControlPlaneApiConfigContext() {
        when(context.getConfig()).thenReturn(ConfigFactory.empty());

        extension.initialize(context);

        verify(webServiceMock).registerResource(eq(CONTROL_PLANE_API_CONTEXT), any(ConsumerPullTransferTokenValidationApiController.class));
    }

    @Test
    void verifyControllerRegisteredOnDeprecatedValidationContext() {
        var config = ConfigFactory.fromMap(Map.of("web.http.validation.port", "8182"));
        when(context.getConfig()).thenReturn(config);

        extension.initialize(context);

        verify(webServiceMock).registerResource(eq("validation"), any(ConsumerPullTransferTokenValidationApiController.class));
    }

    private static KeyPair generateRandomKeyPair() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate()
                .toKeyPair();
    }
}