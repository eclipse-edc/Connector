package org.eclipse.dataspaceconnector.api.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;


class IdResponseDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void verifySerialization() throws JsonProcessingException {
        var assetDto = IdResponseDto.Builder.newInstance().id("assetId").createdAt(1664209047728L).build();

        var str = objectMapper.writeValueAsString(assetDto);

        assertThat(str).isNotNull();

        var deserialized = objectMapper.readValue(str, IdResponseDto.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(assetDto);
    }
}