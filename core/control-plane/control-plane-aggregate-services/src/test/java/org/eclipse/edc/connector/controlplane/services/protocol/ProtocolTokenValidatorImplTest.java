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
 *       Cofinity-X - make participant id extraction dependent on dataspace profile context
 *
 */

package org.eclipse.edc.connector.controlplane.services.protocol;

import org.eclipse.edc.controlplane.ProtocolRemoteMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.context.request.spi.RequestScopeExtractorRegistry;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.protocol.spi.RequestContext;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.UNAUTHORIZED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProtocolTokenValidatorImplTest {

    private final ParticipantAgentService agentService = mock();
    private final IdentityService identityService = mock();
    private final RequestScopeExtractorRegistry requestScopeExtractorRegistry = mock();
    private final DataspaceProfileContextRegistry dataspaceProfileContextRegistry = mock();
    private final ProtocolTokenValidatorImpl validator = new ProtocolTokenValidatorImpl(identityService,
            requestScopeExtractorRegistry, mock(), agentService, dataspaceProfileContextRegistry);

    @BeforeEach
    void setUp() {
        when(requestScopeExtractorRegistry.extractScopes(any(), any())).thenReturn(Result.success(Set.of()));
    }

    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance()
            .id("participantContextId")
            .identity("participantId")
            .build();

    @Test
    void shouldVerifyToken() {
        var participantId = "participantId";
        var protocol = "protocol";
        var participantAgent = new ParticipantAgent("identity", emptyMap(), emptyMap());
        var claimToken = ClaimToken.Builder.newInstance().build();
        var policy = Policy.Builder.newInstance().build();
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().build();
        when(identityService.verifyJwtToken(any(), any(), any())).thenReturn(Result.success(claimToken));
        var dataspaceProfileContext = createDataspaceProfileContext(participantId);
        when(dataspaceProfileContextRegistry.getProfile(any())).thenReturn(dataspaceProfileContext);
        when(agentService.createFor(any(), any())).thenReturn(participantAgent);

        var result = validator.verify(participantContext, tokenRepresentation, policy, new TestMessage());

        assertThat(result).isSucceeded().isSameAs(participantAgent);
        verify(agentService).createFor(claimToken, participantId);
        verify(requestScopeExtractorRegistry).extractScopes(same(policy), argThat(reqContext ->
                reqContext.getMessage().getClass().equals(TestMessage.class) && reqContext.getDirection().equals(RequestContext.Direction.Ingress) &&
                        participantContext.getId().equals(reqContext.getParticipantContextId())));
        verify(dataspaceProfileContextRegistry).getProfile(protocol);
        verify(identityService).verifyJwtToken(
                eq(participantContext.getId()),
                same(tokenRepresentation),
                argThat(verificationContext -> verificationContext.getDataspaceProfileContext().equals(dataspaceProfileContext))
        );
    }

    @Test
    void shouldReturnUnauthorized_whenTokenIsNotValid() {
        when(identityService.verifyJwtToken(any(), any(), any())).thenReturn(Result.failure("failure"));

        var result = validator.verify(participantContext, TokenRepresentation.Builder.newInstance().build(), Policy.Builder.newInstance().build(), new TestMessage());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(UNAUTHORIZED);
    }

    @Test
    void shouldReturnUnauthorized_whenProfileNotSupported() {
        var claimToken = ClaimToken.Builder.newInstance().build();
        var policy = Policy.Builder.newInstance().build();
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().build();

        when(identityService.verifyJwtToken(any(), any(), any())).thenReturn(Result.success(claimToken));
        when(dataspaceProfileContextRegistry.getProfile(any())).thenReturn(null);

        var result = validator.verify(participantContext, tokenRepresentation, policy, new TestMessage());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(UNAUTHORIZED);
    }

    @Test
    void shouldReturnUnauthorized_whenExtractedIdIsNull() {
        var claimToken = ClaimToken.Builder.newInstance().build();
        var policy = Policy.Builder.newInstance().build();
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().build();
        when(identityService.verifyJwtToken(any(), any(), any())).thenReturn(Result.success(claimToken));
        when(dataspaceProfileContextRegistry.getProfile(any())).thenReturn(createDataspaceProfileContext(null));

        var result = validator.verify(participantContext, tokenRepresentation, policy, new TestMessage());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(UNAUTHORIZED);
    }

    private @NonNull DataspaceProfileContext createDataspaceProfileContext(String participantId) {
        return new DataspaceProfileContext(
                "any", new ProtocolVersion("any", "any", "any"), mock(), i -> participantId,
                new JsonLdNamespace("any"), emptyList(), emptyList()
        );
    }

    static class TestMessage extends ProtocolRemoteMessage {

        @Override
        public String getProtocol() {
            return "protocol";
        }

        @Override
        public String getCounterPartyAddress() {
            return "http://connector";
        }

    }

}
