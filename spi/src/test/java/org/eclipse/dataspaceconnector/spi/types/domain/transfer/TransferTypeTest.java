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

package org.eclipse.dataspaceconnector.spi.types.domain.transfer;

import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TransferTypeTest {

    private TypeManager tm;

    @BeforeEach
    void setup() {
        tm = new TypeManager();
        tm.registerTypes(TransferType.class);
    }

    @Test
    void verifySerialization() {
        var tt = TransferType.Builder.transferType()
                .isFinite(false)
                .contentType("someContentType")
                .build();

        var json = tm.writeValueAsString(tt);

        assertThat(json).contains("\"contentType\":\"someContentType\"");
        assertThat(json).contains("\"isFinite\":false");
        //noinspection unchecked
        final Map<String, Object> map = (Map<String, Object>) tm.readValue(json, Map.class);
        assertThat(map).hasSize(2);
        assertThat(map).containsKey("contentType");
        assertThat(map).containsKey("isFinite");
        assertThat(map).doesNotContainKey("edctype");
    }

    @Test
    void verifyDeserialization() {
        var tt = TransferType.Builder.transferType()
                .isFinite(false)
                .contentType("someContentType")
                .build();

        var json = tm.writeValueAsString(tt);

        var restored = tm.readValue(json, TransferType.class);
        assertThat(restored).usingRecursiveComparison().isEqualTo(tt);
        assertThat(restored.isFinite()).isFalse();
        assertThat(restored.getContentType()).isEqualTo("someContentType");
    }

}
