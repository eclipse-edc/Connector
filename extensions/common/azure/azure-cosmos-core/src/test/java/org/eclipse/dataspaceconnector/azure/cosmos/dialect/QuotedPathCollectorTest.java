/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.cosmos.dialect;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


public class QuotedPathCollectorTest {

    static final List<String> RESERVED_WORDS = List.of("value");


    @Test
    void quotedPathCollector_withFinalKeyword() {
        assertThat(Stream.of("obj", "value").collect(QuotedPathCollector.quoteJoining(RESERVED_WORDS))).isEqualTo("obj[\"value\"]");
    }

    @Test
    void quotedPathCollector_withNoKeyword() {
        assertThat(Stream.of("obj", "bar").collect(QuotedPathCollector.quoteJoining(RESERVED_WORDS))).isEqualTo("obj.bar");
    }

    @Test
    void quotedPathCollector_withMiddleKeyword() {
        assertThat(Stream.of("obj", "value", "bar").collect(QuotedPathCollector.quoteJoining(RESERVED_WORDS))).isEqualTo("obj[\"value\"].bar");
    }

    @Test
    void quotedPathCollector_withMultipleKeyword() {
        assertThat(Stream.of("obj", "value", "value").collect(QuotedPathCollector.quoteJoining(RESERVED_WORDS))).isEqualTo("obj[\"value\"][\"value\"]");
    }
}
