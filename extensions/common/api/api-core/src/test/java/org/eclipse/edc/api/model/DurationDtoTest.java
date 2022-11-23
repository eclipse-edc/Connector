/*
 *  Copyright (c) 2022 Amadeus
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

package org.eclipse.edc.api.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DurationDtoTest {

    @Test
    void verifySerialization() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var dto = DurationDto.Builder.newInstance()
                .value(10)
                .unit(TimeUnit.MINUTES.toString())
                .build();

        var str = mapper.writeValueAsString(dto);

        assertThat(str).isNotNull();

        var deserialized = mapper.readValue(str, DurationDto.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(dto);
    }

    @Test
    void verifyToSeconds() {
        var dto = DurationDto.Builder.newInstance()
                .value(10)
                .unit(TimeUnit.MINUTES.toString())
                .build();

        var seconds = dto.toSeconds();

        assertThat(seconds).isEqualTo(600);
    }
}