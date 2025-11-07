/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.decentralizedclaims.spi.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialSubjectTest {

    @Test
    void build_noClaims() {
        assertThatThrownBy(() -> org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject.Builder.newInstance()
                .build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject.Builder.newInstance()
                .claims(null)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void serDes() throws JsonProcessingException {
        var mapper = new ObjectMapper();
        var cred = org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject.Builder.newInstance()
                .claim("key", "val")
                .claim("complex", Map.of("sub", "subval"))
                .build();

        var json = mapper.writeValueAsString(cred);

        assertThat(json).containsIgnoringWhitespaces("sub\": \"subval");

        var deserialized = mapper.readValue(json, CredentialSubject.class);
        assertThat(deserialized.getClaims()).hasSize(2)
                .containsEntry("key", "val")
                .containsKey("complex")
                .hasEntrySatisfying("complex", o -> assertThat(o).isInstanceOf(Map.class));
    }
}