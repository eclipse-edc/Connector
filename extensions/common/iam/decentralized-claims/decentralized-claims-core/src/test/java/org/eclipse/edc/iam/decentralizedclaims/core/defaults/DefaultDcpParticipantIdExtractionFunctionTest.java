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

package org.eclipse.edc.iam.decentralizedclaims.core.defaults;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.core.DcpDefaultServicesExtension.CLAIMTOKEN_VC_KEY;
import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createCredential;

class DefaultDcpParticipantIdExtractionFunctionTest {

    private final DefaultDcpParticipantIdExtractionFunction function = new DefaultDcpParticipantIdExtractionFunction();

    @Test
    void apply_success() {
        var vc = createCredential();

        var result = function.apply(ClaimToken.Builder.newInstance().claim(CLAIMTOKEN_VC_KEY, List.of(vc)).build());

        assertThat(result).isEqualTo(vc.getCredentialSubject().get(0).getId());
    }


    @Test
    void apply_noVcClaim_shouldReturnNull() {
        var result = function.apply(ClaimToken.Builder.newInstance().build());

        assertThat(result).isNull();
    }

    @Test
    void apply_claimIsNotVc_shouldReturnNull() {
        var result = function.apply(ClaimToken.Builder.newInstance().claim(CLAIMTOKEN_VC_KEY, List.of("test")).build());

        assertThat(result).isNull();
    }

}
