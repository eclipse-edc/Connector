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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration test that demonstrates the complete data masking functionality
 * as specified in the requirements.
 */
@DisplayName("Data Masking Integration Tests")
class DataMaskingIntegrationTest {

    private final Monitor monitor = mock(Monitor.class);
    private DataMaskingService dataMaskingService;

    @BeforeEach
    void setUp() {
        dataMaskingService = new DataMaskingServiceImpl(monitor, true);
    }

    @Test
    @DisplayName("Should mask the exact example from requirements")
    void shouldMaskExampleFromRequirements() {
        // given - exact input from requirements
        String inputJson = """
                {
                    "name": "Jonathan Smith",
                    "phone": "+44 7911 123456",
                    "email": "jonathansmith@example.com"
                }
                """;

        // when
        String maskedJson = dataMaskingService.maskJsonData(inputJson);

        // then - verify it matches the expected output format
        assertThat(maskedJson).contains("\"name\":\"J******* S****\"");
        assertThat(maskedJson).contains("\"phone\":\"+** **** ***456\"");
        assertThat(maskedJson).contains("\"email\":\"j************@example.com\"");

        // Verify the original data is not present
        assertThat(maskedJson).doesNotContain("Jonathan Smith");
        assertThat(maskedJson).doesNotContain("+44 7911 123456");
        assertThat(maskedJson).doesNotContain("jonathansmith@example.com");
    }

    @Test
    @DisplayName("Should demonstrate name masking rule - keep initials visible")
    void shouldDemonstrateNameMaskingRule() {
        // Test various name formats
        assertThat(dataMaskingService.maskName("John")).isEqualTo("J***");
        assertThat(dataMaskingService.maskName("John Doe")).isEqualTo("J*** D**");
        assertThat(dataMaskingService.maskName("Mary Jane Watson")).isEqualTo("M*** J*** W*****");
        assertThat(dataMaskingService.maskName("X")).isEqualTo("X");
    }

    @Test
    @DisplayName("Should demonstrate phone masking rule - mask all but last 3 digits")
    void shouldDemonstratePhoneMaskingRule() {
        // Test various phone formats
        assertThat(dataMaskingService.maskPhoneNumber("1234567890")).isEqualTo("*******890");
        assertThat(dataMaskingService.maskPhoneNumber("+1 (555) 123-4567")).isEqualTo("+* (***) ***-*567");
        assertThat(dataMaskingService.maskPhoneNumber("555.123.4567")).isEqualTo("***.***.*567");
        assertThat(dataMaskingService.maskPhoneNumber("+44 7911 123456")).isEqualTo("+** **** ***456");
    }

    @Test
    @DisplayName("Should demonstrate email masking rule - mask before @ leaving first letter")
    void shouldDemonstrateEmailMaskingRule() {
        // Test various email formats
        assertThat(dataMaskingService.maskEmail("john@example.com")).isEqualTo("j***@example.com");
        assertThat(dataMaskingService.maskEmail("jonathansmith@example.com")).isEqualTo("j************@example.com");
        assertThat(dataMaskingService.maskEmail("a@test.co.uk")).isEqualTo("a@test.co.uk");
        assertThat(dataMaskingService.maskEmail("user.name@company.org")).isEqualTo("u********@company.org");
    }

    @Test
    @DisplayName("Should handle complex nested JSON structures")
    void shouldHandleComplexJsonStructures() {
        // given
        String complexJson = """
                {
                    "users": [
                        {
                            "id": 1,
                            "name": "Alice Johnson",
                            "contact": {
                                "email": "alice.johnson@company.com",
                                "phone": "+1-555-0123-456"
                            }
                        },
                        {
                            "id": 2,
                            "name": "Bob Smith",
                            "contact": {
                                "email": "bob@company.com",
                                "phone": "555-987-6543"
                            }
                        }
                    ],
                    "metadata": {
                        "total": 2,
                        "timestamp": "2025-07-14T10:00:00Z"
                    }
                }
                """;

        // when
        String maskedJson = dataMaskingService.maskJsonData(complexJson);

        // then
        // Verify sensitive data is masked
        assertThat(maskedJson).contains("\"name\":\"A**** J******\"");
        assertThat(maskedJson).contains("\"name\":\"B** S****\"");
        assertThat(maskedJson).contains("\"email\":\"a************@company.com\"");
        assertThat(maskedJson).contains("\"email\":\"b**@company.com\"");
        assertThat(maskedJson).contains("\"phone\":\"+*-***-****-456\"");
        assertThat(maskedJson).contains("\"phone\":\"***-***-*543\"");

        // Verify non-sensitive data is preserved
        assertThat(maskedJson).contains("\"id\":1");
        assertThat(maskedJson).contains("\"id\":2");
        assertThat(maskedJson).contains("\"total\":2");
        assertThat(maskedJson).contains("\"timestamp\":\"2025-07-14T10:00:00Z\"");
    }

    @Test
    @DisplayName("Should work with different field name variations")
    void shouldHandleFieldNameVariations() {
        // given
        String jsonWithVariations = """
                {
                    "name": "John Doe",
                    "phone": "123-456-7890",
                    "phoneNumber": "987-654-3210",
                    "phone_number": "555-555-5555",
                    "email": "john@example.com",
                    "emailAddress": "john.doe@company.com",
                    "email_address": "jdoe@test.org"
                }
                """;

        // when
        String maskedJson = dataMaskingService.maskJsonData(jsonWithVariations);

        // then
        assertThat(maskedJson).contains("\"name\":\"J*** D**\"");
        assertThat(maskedJson).contains("\"phone\":\"***-***-*890\"");
        assertThat(maskedJson).contains("\"phoneNumber\":\"***-***-*210\"");
        assertThat(maskedJson).contains("\"phone_number\":\"***-***-*555\"");
        assertThat(maskedJson).contains("\"email\":\"j***@example.com\"");
        assertThat(maskedJson).contains("\"emailAddress\":\"j*******@company.com\"");
        assertThat(maskedJson).contains("\"email_address\":\"j***@test.org\"");
    }

    @Test
    @DisplayName("Should preserve JSON structure and non-sensitive data")
    void shouldPreserveJsonStructureAndNonSensitiveData() {
        // given
        String mixedJson = """
                {
                    "id": 12345,
                    "name": "Test User",
                    "email": "test@example.com",
                    "age": 30,
                    "address": "123 Main Street",
                    "isActive": true,
                    "score": 95.5,
                    "tags": ["customer", "premium"],
                    "metadata": null
                }
                """;

        // when
        String maskedJson = dataMaskingService.maskJsonData(mixedJson);

        // then
        // Sensitive data should be masked
        assertThat(maskedJson).contains("\"name\":\"T*** U***\"");
        assertThat(maskedJson).contains("\"email\":\"t***@example.com\"");

        // Non-sensitive data should be preserved exactly
        assertThat(maskedJson).contains("\"id\":12345");
        assertThat(maskedJson).contains("\"age\":30");
        assertThat(maskedJson).contains("\"address\":\"123 Main Street\"");
        assertThat(maskedJson).contains("\"isActive\":true");
        assertThat(maskedJson).contains("\"score\":95.5");
        assertThat(maskedJson).contains("\"tags\":[\"customer\",\"premium\"]");
        assertThat(maskedJson).contains("\"metadata\":null");
    }

    @Test
    @DisplayName("Should be configurable to disable masking")
    void shouldBeConfigurableToDisableMasking() {
        // given - service with masking disabled
        var disabledService = new DataMaskingServiceImpl(monitor, false);
        String jsonData = """
                {
                    "name": "Jonathan Smith",
                    "phone": "+44 7911 123456",
                    "email": "jonathansmith@example.com"
                }
                """;

        // when
        String result = disabledService.maskJsonData(jsonData);

        // then - data should remain unchanged
        assertThat(result).contains("\"name\":\"Jonathan Smith\"");
        assertThat(result).contains("\"phone\":\"+44 7911 123456\"");
        assertThat(result).contains("\"email\":\"jonathansmith@example.com\"");
    }

    @Test
    @DisplayName("Should be configurable for specific fields only")
    void shouldBeConfigurableForSpecificFields() {
        // given - service configured to mask only name and email
        var customService = new DataMaskingServiceImpl(monitor, true, "name", "email");
        String jsonData = """
                {
                    "name": "Jonathan Smith",
                    "phone": "+44 7911 123456",
                    "email": "jonathansmith@example.com"
                }
                """;

        // when
        String result = customService.maskJsonData(jsonData);

        // then
        assertThat(result).contains("\"name\":\"J******* S****\""); // masked
        assertThat(result).contains("\"email\":\"j************@example.com\""); // masked
        assertThat(result).contains("\"phone\":\"+44 7911 123456\""); // not masked
    }
}
