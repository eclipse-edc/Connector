/*
 *  Copyright (c) 2024 Amadeus
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

package org.eclipse.edc.iam.identitytrust.core.defaults;

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.core.DcpDefaultServicesExtension.CLAIMTOKEN_VC_KEY;
import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createCredential;

class DefaultDcpParticipantAgentServiceExtensionTest {

    private final DefaultDcpParticipantAgentServiceExtension extension = new DefaultDcpParticipantAgentServiceExtension();

    @Test
    void attributesFor_success() {
        var vc = createCredential();

        var attributes = extension.attributesFor(ClaimToken.Builder.newInstance().claim(CLAIMTOKEN_VC_KEY, List.of(vc)).build());

        assertThat(attributes).containsExactlyEntriesOf(Map.of(ParticipantAgent.PARTICIPANT_IDENTITY, vc.getCredentialSubject().stream().findFirst().orElseThrow().getId()));
    }


    @Test
    void attributesFor_noVcClaim_shouldReturnEmptyMap() {
        var attributes = extension.attributesFor(ClaimToken.Builder.newInstance().build());

        assertThat(attributes).isEmpty();
    }

    @Test
    void attributesFor_claimIsNotVc_shouldReturnEmptyMap() {
        var attributes = extension.attributesFor(ClaimToken.Builder.newInstance().claim(CLAIMTOKEN_VC_KEY, List.of("test")).build());

        assertThat(attributes).isEmpty();
    }

}
