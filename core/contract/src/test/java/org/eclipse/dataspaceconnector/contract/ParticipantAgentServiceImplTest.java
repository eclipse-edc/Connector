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
package org.eclipse.dataspaceconnector.contract;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.spi.contract.ParticipantAgentServiceExtension;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * verifies the {@link ParticipantAgentServiceImpl}.
 */
class ParticipantAgentServiceImplTest {

    @Test
    void verifyRegisteredExtensionIsInvoked() {
        ParticipantAgentServiceExtension extension = EasyMock.createMock(ParticipantAgentServiceExtension.class);
        EasyMock.expect(extension.attributesFor(EasyMock.isA(ClaimToken.class))).andReturn(Map.of("foo", "bar"));
        EasyMock.replay(extension);

        var participantAgentService = new ParticipantAgentServiceImpl();
        participantAgentService.register(extension);

        var participantAgent = participantAgentService.createFor(ClaimToken.Builder.newInstance().build());

        assertThat(participantAgent.getAttributes().containsKey("foo")).isTrue();

        EasyMock.verify(extension);
    }
}
