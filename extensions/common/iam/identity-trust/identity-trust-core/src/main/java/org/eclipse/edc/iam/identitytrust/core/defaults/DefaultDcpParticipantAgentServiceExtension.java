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

import org.eclipse.edc.iam.identitytrust.spi.DcpParticipantAgentServiceExtension;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.identitytrust.core.DcpDefaultServicesExtension.CLAIMTOKEN_VC_KEY;
import static org.eclipse.edc.participant.spi.ParticipantAgent.PARTICIPANT_IDENTITY;

/**
 * Retrieve subject id from the list of {@link VerifiableCredential} and set the
 * PARTICIPANT_IDENTITY attribute accordingly.
 */
public class DefaultDcpParticipantAgentServiceExtension implements DcpParticipantAgentServiceExtension {
    @Override
    public @NotNull Map<String, String> attributesFor(ClaimToken token) {
        return ofNullable(token.getListClaim(CLAIMTOKEN_VC_KEY)).orElse(emptyList())
                .stream()
                .filter(o -> o instanceof VerifiableCredential)
                .map(o -> (VerifiableCredential) o)
                .flatMap(vc -> vc.getCredentialSubject().stream())
                .map(CredentialSubject::getId)
                .filter(Objects::nonNull)
                .findFirst()
                .map(sub -> Map.of(PARTICIPANT_IDENTITY, sub))
                .orElse(emptyMap());
    }

}
