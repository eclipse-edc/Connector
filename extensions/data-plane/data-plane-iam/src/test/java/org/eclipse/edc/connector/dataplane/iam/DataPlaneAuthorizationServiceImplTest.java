/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.iam;

import org.eclipse.edc.connector.dataplane.iam.service.DataPlaneAuthorizationServiceImpl;
import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessControlService;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessTokenService;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.Clock;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DataPlaneAuthorizationServiceImplTest {

    public static final String OWN_PARTICIPANT_ID = "test-ownParticipantId";
    private final DataPlaneAccessTokenService accessTokenService = mock();
    private final PublicEndpointGeneratorService endpointGenerator = mock();
    private final DataPlaneAccessControlService accessControlService = mock();
    private final DataPlaneAuthorizationServiceImpl authorizationService = new DataPlaneAuthorizationServiceImpl(accessTokenService, endpointGenerator, accessControlService, OWN_PARTICIPANT_ID, Clock.systemUTC());

    @BeforeEach
    void setup() {
        when(endpointGenerator.generateFor(any(), any())).thenReturn(Result.success(Endpoint.url("http://example.com")));
    }

    @Test
    void createEndpointDataReference() {
        when(accessTokenService.obtainToken(any(), any(), anyMap())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("footoken").build()));
        var source = createDataAddress();
        var startMsg = dataFlowBuilder()
                .transferType(new TransferType("DestinationType", FlowType.PULL))
                .participantId("participantId")
                .source(source)
                .build();

        var result = authorizationService.createEndpointDataReference(startMsg);
        assertThat(result).isSucceeded()
                .satisfies(da -> {
                    assertThat(da.getType()).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
                    assertThat(da.getStringProperty("endpoint")).isEqualTo("http://example.com");
                    assertThat(da.getStringProperty("endpointType")).isEqualTo(da.getType());
                    assertThat(da.getStringProperty("authorization")).isEqualTo("footoken");
                });

        var requiredClaims = Set.of(JWT_ID, AUDIENCE, ISSUER, SUBJECT, ISSUED_AT);
        verify(accessTokenService).obtainToken(ArgumentMatchers.assertArg(tp -> {
            assertThat(tp.getClaims().keySet()).containsAll(requiredClaims);
            assertThat(tp.getStringClaim(AUDIENCE)).isEqualTo("participantId");
            assertThat(tp.getStringClaim(ISSUER)).isEqualTo(OWN_PARTICIPANT_ID);
            assertThat(tp.getStringClaim(SUBJECT)).isEqualTo(OWN_PARTICIPANT_ID);
            assertThat(tp.getClaims().get(ISSUED_AT)).isNotNull();
        }), any(), argThat(m ->
                m.containsKey("agreement_id") &&
                        m.containsKey("participant_id") &&
                        m.containsKey("asset_id") &&
                        m.containsKey("process_id") &&
                        m.containsKey("flow_type")));
        verify(endpointGenerator).generateFor("DestinationType", source);
    }

    @Test
    void createEndpointDataReference_withAuthType() {
        when(accessTokenService.obtainToken(any(), any(), anyMap())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance()
                .token("footoken")
                .additional(Map.of("authType", "bearer", "fizz", "buzz"))
                .build()));
        var dataFlow = dataFlowBuilder().build();

        var result = authorizationService.createEndpointDataReference(dataFlow);

        assertThat(result).isSucceeded()
                .satisfies(da -> {
                    assertThat(da.getType()).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
                    assertThat(da.getStringProperty("endpoint")).isEqualTo("http://example.com");
                    assertThat(da.getStringProperty("authorization")).isEqualTo("footoken");
                    assertThat(da.getStringProperty("authType")).isEqualTo("bearer");
                    assertThat(da.getStringProperty("fizz")).isEqualTo("buzz");
                });
    }

    @Test
    void createEndpointDataReference_tokenServiceFails() {
        when(accessTokenService.obtainToken(any(), any(), anyMap())).thenReturn(Result.failure("test-failure"));
        var dataFlow = dataFlowBuilder().build();

        var result = authorizationService.createEndpointDataReference(dataFlow);

        assertThat(result).isFailed()
                .detail().isEqualTo("test-failure");
    }

    @Test
    void authorize() {
        var claimToken = ClaimToken.Builder.newInstance().build();
        var address = DataAddress.Builder.newInstance().type("test-type").build();
        when(accessTokenService.resolve(eq("foo-token"))).thenReturn(Result.success(new AccessTokenData("test-id",
                claimToken,
                address)));
        when(accessControlService.checkAccess(eq(claimToken), eq(address), any(), anyMap())).thenReturn(Result.success());

        var result = authorizationService.authorize("foo-token", Map.of());

        assertThat(result).isSucceeded();
        verify(accessTokenService).resolve(eq("foo-token"));
        verify(accessControlService).checkAccess(eq(claimToken), eq(address), any(), anyMap());
        verifyNoMoreInteractions(accessTokenService, accessControlService);
    }

    @Test
    void authorize_tokenNotFound() {
        when(accessTokenService.resolve(eq("foo-token"))).thenReturn(Result.failure("not found"));

        var result = authorizationService.authorize("foo-token", Map.of());

        assertThat(result).isFailed().detail().isEqualTo("not found");
        verify(accessTokenService).resolve(eq("foo-token"));
        verifyNoMoreInteractions(accessTokenService, accessControlService);
    }

    @Test
    void authorize_accessNotGranted() {
        var claimToken = ClaimToken.Builder.newInstance().build();
        var address = DataAddress.Builder.newInstance().type("test-type").build();
        when(accessTokenService.resolve(eq("foo-token"))).thenReturn(Result.success(new AccessTokenData("test-id",
                claimToken,
                address)));
        when(accessControlService.checkAccess(eq(claimToken), eq(address), any(), anyMap())).thenReturn(Result.failure("not granted"));

        var result = authorizationService.authorize("foo-token", Map.of());

        assertThat(result).isFailed().detail().isEqualTo("not granted");
        verify(accessTokenService).resolve(eq("foo-token"));
        verify(accessControlService).checkAccess(eq(claimToken), eq(address), any(), anyMap());
        verifyNoMoreInteractions(accessTokenService, accessControlService);
    }

    @Test
    void revoke() {
        when(accessTokenService.revoke(eq("id"), eq("reason"))).thenReturn(ServiceResult.success());

        var result = authorizationService.revokeEndpointDataReference("id", "reason");

        assertThat(result).isSucceeded();
        verify(accessTokenService).revoke(eq("id"), eq("reason"));
        verifyNoMoreInteractions(accessTokenService, accessControlService);
    }

    @Test
    void revoke_error() {
        when(accessTokenService.revoke(eq("id"), eq("reason"))).thenReturn(ServiceResult.notFound("failure"));

        var result = authorizationService.revokeEndpointDataReference("id", "reason");

        assertThat(result).isFailed().detail().contains("failure");

        verify(accessTokenService).revoke(eq("id"), eq("reason"));
        verifyNoMoreInteractions(accessTokenService, accessControlService);
    }

    private DataAddress createDataAddress() {
        return DataAddress.Builder.newInstance().type("test-src").build();
    }

    private DataFlow.Builder dataFlowBuilder() {
        return DataFlow.Builder.newInstance().transferType(new TransferType("any", FlowType.PULL));
    }
}
