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

package org.eclipse.edc.connector.transfer.dataplane;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.eclipse.edc.connector.api.control.configuration.ControlApiConfiguration;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.transfer.dataplane.api.ConsumerPullTransferTokenValidationApiController;
import org.eclipse.edc.connector.transfer.dataplane.flow.ConsumerPullTransferDataFlowController;
import org.eclipse.edc.connector.transfer.dataplane.flow.ProviderPushTransferDataFlowController;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.KeyPairFactory;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.TOKEN_SIGNER_PRIVATE_KEY_ALIAS;
import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.TOKEN_VERIFIER_PUBLIC_KEY_ALIAS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class TransferDataPlaneCoreExtensionTest {

    private static final String CONTROL_PLANE_API_CONTEXT = "control";

    private final Vault vault = mock(Vault.class);
    private final WebService webService = mock(WebService.class);
    private final DataFlowManager dataFlowManager = mock(DataFlowManager.class);
    private final KeyPairFactory keyPairFactory = mock();
    private KeyPair keypair;
    private ServiceExtensionContext context;
    private TransferDataPlaneCoreExtension extension;

    private static KeyPair keyPair() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate()
                .toKeyPair();
    }

    private static String publicKeyPem() throws IOException {
        return new String(Objects.requireNonNull(TransferDataPlaneCoreExtensionTest.class.getClassLoader().getResourceAsStream("rsa-pubkey.pem"))
                .readAllBytes());
    }

    @BeforeEach
    public void setUp(ServiceExtensionContext context, ObjectFactory factory) throws JOSEException {
        keypair = keyPair();
        var monitor = mock(Monitor.class);
        var controlApiConfigurationMock = mock(ControlApiConfiguration.class);
        when(controlApiConfigurationMock.getContextAlias()).thenReturn(CONTROL_PLANE_API_CONTEXT);

        context.registerService(PrivateKeyResolver.class, mock(PrivateKeyResolver.class));
        context.registerService(Vault.class, mock(Vault.class));
        context.registerService(WebService.class, webService);
        context.registerService(ContractNegotiationStore.class, mock(ContractNegotiationStore.class));
        context.registerService(RemoteMessageDispatcherRegistry.class, mock(RemoteMessageDispatcherRegistry.class));
        context.registerService(DataFlowManager.class, dataFlowManager);
        context.registerService(DataEncrypter.class, mock(DataEncrypter.class));
        context.registerService(ControlApiConfiguration.class, controlApiConfigurationMock);
        context.registerService(DataPlaneClient.class, mock(DataPlaneClient.class));
        context.registerService(Vault.class, vault);
        context.registerService(KeyPairFactory.class, keyPairFactory);

        this.context = spy(context); //used to inject the config
        when(this.context.getMonitor()).thenReturn(monitor);

        extension = factory.constructInstance(TransferDataPlaneCoreExtension.class);
    }

    @Test
    void verifyInitializeSuccess() throws IOException, JOSEException {
        var publicKeyAlias = "publicKey";
        var privateKeyAlias = "privateKey";
        var config = mock(Config.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getString(TOKEN_VERIFIER_PUBLIC_KEY_ALIAS, null)).thenReturn(publicKeyAlias);
        when(config.getString(TOKEN_SIGNER_PRIVATE_KEY_ALIAS, null)).thenReturn(privateKeyAlias);
        when(vault.resolveSecret(publicKeyAlias)).thenReturn(publicKeyPem());
        when(keyPairFactory.fromConfig(publicKeyAlias, privateKeyAlias)).thenReturn(Result.success(keypair));

        extension.initialize(context);

        verify(dataFlowManager).register(any(ConsumerPullTransferDataFlowController.class));
        verify(dataFlowManager).register(any(ProviderPushTransferDataFlowController.class));
        verify(webService).registerResource(eq(CONTROL_PLANE_API_CONTEXT), any(ConsumerPullTransferTokenValidationApiController.class));
    }
}
