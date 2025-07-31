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
 *       Cofinity-X - make participant id extraction dependent on dataspace profile context
 *
 */

package org.eclipse.edc.iam.identitytrust.core.defaults;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction;
import org.eclipse.edc.spi.iam.ClaimToken;

import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.identitytrust.core.DcpDefaultServicesExtension.CLAIMTOKEN_VC_KEY;

/**
 * Extracts the participant id from a ClaimToken by retrieving the credential subject id from a
 * list of {@link VerifiableCredential}s.
 */
public class DefaultDcpParticipantIdExtractionFunction implements DefaultParticipantIdExtractionFunction {
    @Override
    public String apply(ClaimToken claimToken) {
        return ofNullable(claimToken.getListClaim(CLAIMTOKEN_VC_KEY)).orElse(emptyList())
                .stream()
                .filter(o -> o instanceof VerifiableCredential)
                .map(o -> (VerifiableCredential) o)
                .flatMap(vc -> vc.getCredentialSubject().stream())
                .map(CredentialSubject::getId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
