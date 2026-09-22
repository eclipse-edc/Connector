/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.partner.spi;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartnerGroupTest {

    @Test
    void build_whenIdNotProvided_shouldGenerateUuid() {
        var group = PartnerGroup.Builder.newInstance().name("gold").build();

        assertThat(group.getId()).isNotNull();
        assertThatCode(() -> UUID.fromString(group.getId())).doesNotThrowAnyException();
    }

    @Test
    void build_whenNameMissing_shouldThrow() {
        assertThatThrownBy(() -> PartnerGroup.Builder.newInstance().id("id").build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");
    }

    @Test
    void toBuilder_shouldCopyAllFields() {
        var group = PartnerGroup.Builder.newInstance()
                .id("id").participantContextId("pc").name("gold").description("desc")
                .properties(Map.of("region", "eu"))
                .build();

        var copy = group.toBuilder().build();

        assertThat(copy).usingRecursiveComparison().isEqualTo(group);
    }
}
