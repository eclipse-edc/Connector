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

package org.eclipse.edc.connector.transfer.dataplane.proxy;

import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.connector.transfer.dataplane.spi.token.ConsumerPullTokenExpirationDateFunction;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.sql.Date;
import java.time.Instant;
import java.util.UUID;

import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.DATA_ADDRESS;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsumerPullDataPlaneProxyResolverTest {

    private static final TypeManager TYPE_MANAGER = new TypeManager();

    private final DataEncrypter dataEncrypter = mock();
    private final TokenGenerationService tokenGenerationService = mock();
    private final ConsumerPullTokenExpirationDateFunction tokenExpirationDateFunction = mock();

    private final ConsumerPullDataPlaneProxyResolver resolver = new ConsumerPullDataPlaneProxyResolver(dataEncrypter, TYPE_MANAGER, tokenGenerationService, Mockito::mock, () -> "test-public-key", tokenExpirationDateFunction);

    private static DataAddress dataAddress() {
        return DataAddress.Builder.newInstance().type(UUID.randomUUID().toString()).build();
    }

    @Test
    void verifyToDataAddressSuccess() {
        var address = dataAddress();
        var encryptedAddress = "encryptedAddress";
        var expiration = Date.from(Instant.now().plusSeconds(100));
        var proxyUrl = "test.proxy.url";
        var token = "token-test";
        var request = dataRequest();
        var instance = DataPlaneInstance.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .url("http://some.test.url")
                .property("publicApiUrl", proxyUrl)
                .build();

        var captor = ArgumentCaptor.forClass(TokenDecorator[].class);
        when(dataEncrypter.encrypt(TYPE_MANAGER.writeValueAsString(address))).thenReturn(encryptedAddress);
        when(tokenExpirationDateFunction.expiresAt(address, request.getContractId())).thenReturn(Result.success(expiration));
        when(tokenGenerationService.generate(any(), captor.capture()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(token).build()));

        var result = resolver.toDataAddress(request, address, instance);

        assertThat(result.succeeded()).isTrue();
        var proxyAddress = result.getContent();
        assertThat(proxyAddress.getType()).isEqualTo(EndpointDataReference.EDR_SIMPLE_TYPE);
        assertThat(proxyAddress.getProperties())
                .containsEntry(EndpointDataReference.ID, request.getId())
                .containsEntry(EndpointDataReference.CONTRACT_ID, request.getContractId())
                .containsEntry(EndpointDataReference.ENDPOINT, proxyUrl)
                .containsEntry(EndpointDataReference.AUTH_KEY, HttpHeaders.AUTHORIZATION)
                .containsEntry(EndpointDataReference.AUTH_CODE, token);

        var decorators = captor.getValue();

        assertThat(decorators).anySatisfy(decorator -> assertThat(decorator.claims())
                .containsEntry(DATA_ADDRESS, encryptedAddress)
                .containsEntry(EXPIRATION_TIME, expiration));

        assertThat(decorators).anySatisfy(decorator -> assertThat(decorator.headers())
                .containsEntry("kid", "test-public-key"));
    }

    @Test
    void verifyToDataAddressReturnsFailureIfMissingPublicApiUrl() {
        var instance = DataPlaneInstance.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .url("http://some.test.url")
                .build();

        var result = resolver.toDataAddress(dataRequest(), dataAddress(), instance);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).isEqualTo("Missing property `https://w3id.org/edc/v0.0.1/ns/publicApiUrl` (deprecated: `publicApiUrl`) in DataPlaneInstance");
    }

    @Test
    void verifyToDataAddressReturnsFailureIfTokenExpirationDateFunctionFails() {
        var address = dataAddress();
        var errorMsg = "error test";
        var instance = DataPlaneInstance.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .url("http://some.test.url")
                .property("publicApiUrl", "test.proxy.url")
                .build();

        when(dataEncrypter.encrypt(TYPE_MANAGER.writeValueAsString(address))).thenReturn("encryptedAddress");
        when(tokenExpirationDateFunction.expiresAt(any(), any())).thenReturn(Result.failure(errorMsg));

        var result = resolver.toDataAddress(dataRequest(), address, instance);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains(errorMsg);
    }

    @Test
    void verifyToDataAddressReturnsFailureIfTokenGenerationFails() {
        var address = dataAddress();
        var errorMsg = "error test";
        var request = dataRequest();
        var expiration = Date.from(Instant.now().plusSeconds(100));
        var instance = DataPlaneInstance.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .url("http://some.test.url")
                .property("publicApiUrl", "test.proxy.url")
                .build();

        when(dataEncrypter.encrypt(TYPE_MANAGER.writeValueAsString(address))).thenReturn("encryptedAddress");
        when(tokenExpirationDateFunction.expiresAt(address, request.getContractId())).thenReturn(Result.success(expiration));
        when(tokenGenerationService.generate(any(), any(TokenDecorator[].class))).thenReturn(Result.failure(errorMsg));

        var result = resolver.toDataAddress(request, address, instance);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains(errorMsg);
    }

    private DataRequest dataRequest() {
        return DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("protocol")
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .connectorAddress("test.connector.address")
                .processId(UUID.randomUUID().toString())
                .destinationType(HTTP_PROXY)
                .build();
    }
}