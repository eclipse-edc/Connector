/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Cofinity-X - make participant id extraction dependent on dataspace profile context
 *
 */

package org.eclipse.edc.connector.core.agent;

import org.eclipse.edc.participant.spi.ParticipantAgentServiceExtension;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.participant.spi.ParticipantAgent.PARTICIPANT_IDENTITY;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * verifies the {@link ParticipantAgentServiceImpl}.
 */
class ParticipantAgentServiceImplTest {

    @Test
    void verifyRegisteredExtensionIsInvoked() {
        ParticipantAgentServiceExtension extension = mock(ParticipantAgentServiceExtension.class);
        when(extension.attributesFor(isA(ClaimToken.class))).thenReturn(Map.of("foo", "bar"));

        var participantAgentService = new ParticipantAgentServiceImpl();
        participantAgentService.register(extension);

        var participantAgent = participantAgentService.createFor(ClaimToken.Builder.newInstance().build(), "test-participant");

        assertThat(participantAgent.getAttributes().containsKey("foo")).isTrue();
        verify(extension).attributesFor(isA(ClaimToken.class));
    }
    
    @Test
    void verifyIdentityIsAddedToParticipantAgent() {
        var participantAgentService = new ParticipantAgentServiceImpl();
        var participantId = "test-participant";
        
        var participantAgent = participantAgentService.createFor(ClaimToken.Builder.newInstance().build(), participantId);
        
        assertThat(participantAgent.getAttributes()).containsEntry(PARTICIPANT_IDENTITY, participantId);
    }

    @Test
    void verifyGivenIdentityOverridesIdentityCreatedByExtension() {
        var participantAgentService = new ParticipantAgentServiceImpl();
        var extensionParticipantId = "overridden-participant";
        var givenParticipantId = "test-participant";

        var extension = mock(ParticipantAgentServiceExtension.class);
        when(extension.attributesFor(isA(ClaimToken.class))).thenReturn(Map.of(PARTICIPANT_IDENTITY, extensionParticipantId));
        participantAgentService.register(extension);

        var agent = participantAgentService.createFor(ClaimToken.Builder.newInstance().build(), givenParticipantId);

        assertThat(agent.getIdentity())
                .isNotEqualTo(extensionParticipantId)
                .isEqualTo(givenParticipantId);
    }
}
