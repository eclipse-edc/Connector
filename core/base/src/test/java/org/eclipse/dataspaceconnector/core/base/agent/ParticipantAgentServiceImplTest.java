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
 *
 */

package org.eclipse.dataspaceconnector.core.base.agent;

import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgentServiceExtension;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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

        var participantAgent = participantAgentService.createFor(ClaimToken.Builder.newInstance().build());

        assertThat(participantAgent.getAttributes().containsKey("foo")).isTrue();
        verify(extension).attributesFor(isA(ClaimToken.class));
    }
}
