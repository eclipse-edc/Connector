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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartnerTest {

    @Test
    void build_whenIdNotProvided_shouldGenerateUuid() {
        var partner = Partner.Builder.newInstance().identity("did:web:x").build();

        assertThat(partner.getId()).isNotNull();
        assertThatCode(() -> UUID.fromString(partner.getId())).doesNotThrowAnyException();
    }

    @Test
    void build_whenIdentityMissing_shouldThrow() {
        assertThatThrownBy(() -> Partner.Builder.newInstance().id("id").build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("identity");
    }

    @Test
    void toBuilder_shouldCopyAllFields() {
        var partner = Partner.Builder.newInstance()
                .id("id").participantContextId("pc").identity("did:web:x").name("name")
                .properties(Map.of("businessId", "BID-0001"))
                .groupIds(Set.of("gold"))
                .build();

        var copy = partner.toBuilder().build();

        assertThat(copy).usingRecursiveComparison().isEqualTo(partner);
        assertThat(copy.isInGroup("gold")).isTrue();
        assertThat(copy.isInGroup("silver")).isFalse();
    }

    @Test
    void collections_shouldBeUnmodifiable() {
        var partner = Partner.Builder.newInstance().identity("did:web:x").groupId("g").property("k", "v").build();

        assertThatThrownBy(() -> partner.getGroupIds().add("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> partner.getProperties().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
    }
}
