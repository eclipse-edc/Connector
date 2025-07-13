/*
 *  Copyright (c) 2025 Eclipse EDC Contributors
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Eclipse EDC Contributors - Data Masking Extension
 *
 */

package org.eclipse.edc.connector.datamasking;

import org.eclipse.edc.connector.datamasking.spi.DataMaskingService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataMaskingTransformerTest {

    private final DataMaskingService dataMaskingService = mock(DataMaskingService.class);
    private final Monitor monitor = mock(Monitor.class);
    private final TransformerContext transformerContext = mock(TransformerContext.class);
    private DataMaskingTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new DataMaskingTransformer(dataMaskingService, monitor);
    }

    @Test
    void shouldReturnCorrectTypes() {
        assertThat(transformer.getInputType()).isEqualTo(String.class);
        assertThat(transformer.getOutputType()).isEqualTo(String.class);
    }

    @Test
    void shouldTransformJsonWithSensitiveFields() {
        // given
        String input = """
                {
                    "name": "Jonathan Smith",
                    "phone": "+44 7911 123456",
                    "email": "jonathansmith@example.com"
                }
                """;
        String expectedOutput = """
                {
                    "name": "J******* S****",
                    "phone": "+** **** ***456",
                    "email": "j************@example.com"
                }
                """;

        when(dataMaskingService.maskJsonData(input)).thenReturn(expectedOutput);

        // when
        String result = transformer.transform(input, transformerContext);

        // then
        assertThat(result).isEqualTo(expectedOutput);
        verify(dataMaskingService).maskJsonData(input);
    }

    @Test
    void shouldNotTransformNonJsonData() {
        // given
        String input = "This is just plain text";

        // when
        String result = transformer.transform(input, transformerContext);

        // then
        assertThat(result).isEqualTo(input);
        verify(dataMaskingService, never()).maskJsonData(anyString());
    }

    @Test
    void shouldNotTransformJsonWithoutSensitiveFields() {
        // given
        String input = """
                {
                    "id": 123,
                    "status": "active",
                    "count": 42
                }
                """;

        // when
        String result = transformer.transform(input, transformerContext);

        // then
        assertThat(result).isEqualTo(input);
        verify(dataMaskingService, never()).maskJsonData(anyString());
    }

    @Test
    void shouldTransformJsonArrayWithSensitiveFields() {
        // given
        String input = """
                [
                    {
                        "name": "John Doe",
                        "email": "john@example.com"
                    }
                ]
                """;
        String expectedOutput = """
                [
                    {
                        "name": "J*** D**",
                        "email": "j***@example.com"
                    }
                ]
                """;

        when(dataMaskingService.maskJsonData(input)).thenReturn(expectedOutput);

        // when
        String result = transformer.transform(input, transformerContext);

        // then
        assertThat(result).isEqualTo(expectedOutput);
        verify(dataMaskingService).maskJsonData(input);
    }

    @Test
    void shouldHandleNullInput() {
        // when
        String result = transformer.transform(null, transformerContext);

        // then
        assertThat(result).isNull();
        verify(dataMaskingService, never()).maskJsonData(anyString());
    }

    @Test
    void shouldHandleEmptyInput() {
        // when
        String result = transformer.transform("", transformerContext);

        // then
        assertThat(result).isEqualTo("");
        verify(dataMaskingService, never()).maskJsonData(anyString());
    }

    @Test
    void shouldHandleExceptionGracefully() {
        // given
        String input = """
                {
                    "name": "John Doe",
                    "email": "john@example.com"
                }
                """;

        when(dataMaskingService.maskJsonData(input)).thenThrow(new RuntimeException("Masking error"));

        // when
        String result = transformer.transform(input, transformerContext);

        // then
        assertThat(result).isEqualTo(input);
        verify(transformerContext).reportProblem("Data masking failed: Masking error");
    }

    @Test
    void shouldDetectVariousFieldNameFormats() {
        // Test different field name formats that should trigger masking
        String[] inputs = {
                """
                        { "name": "John" }
                        """,
                """
                        { "phone": "123456" }
                        """,
                """
                        { "email": "test@example.com" }
                        """,
                """
                        { "phoneNumber": "123456" }
                        """,
                """
                        { "phone_number": "123456" }
                        """,
                """
                        { "emailAddress": "test@example.com" }
                        """,
                """
                        { "email_address": "test@example.com" }
                        """
        };

        for (String input : inputs) {
            when(dataMaskingService.maskJsonData(input)).thenReturn(input);

            String result = transformer.transform(input, transformerContext);

            verify(dataMaskingService).maskJsonData(input);
        }
    }
}
