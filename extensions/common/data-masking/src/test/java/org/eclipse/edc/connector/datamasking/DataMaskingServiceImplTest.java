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

import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DataMaskingServiceImplTest {

    private final Monitor monitor = mock(Monitor.class);
    private DataMaskingServiceImpl dataMaskingService;

    @BeforeEach
    void setUp() {
        dataMaskingService = new DataMaskingServiceImpl(monitor, true);
    }

    @Test
    void shouldMaskName_keepInitials() {
        // given
        String name = "Jonathan Smith";

        // when
        String maskedName = dataMaskingService.maskName(name);

        // then
        assertThat(maskedName).isEqualTo("J******* S****");
    }

    @Test
    void shouldMaskName_singleName() {
        // given
        String name = "John";

        // when
        String maskedName = dataMaskingService.maskName(name);

        // then
        assertThat(maskedName).isEqualTo("J***");
    }

    @Test
    void shouldMaskName_threeParts() {
        // given
        String name = "John Michael Smith";

        // when
        String maskedName = dataMaskingService.maskName(name);

        // then
        assertThat(maskedName).isEqualTo("J*** M****** S****");
    }

    @Test
    void shouldMaskName_singleCharacterParts() {
        // given
        String name = "J A Smith";

        // when
        String maskedName = dataMaskingService.maskName(name);

        // then
        assertThat(maskedName).isEqualTo("J A S****");
    }

    @Test
    void shouldMaskName_handleNullAndEmpty() {
        assertThat(dataMaskingService.maskName(null)).isNull();
        assertThat(dataMaskingService.maskName("")).isEqualTo("");
        assertThat(dataMaskingService.maskName("   ")).isEqualTo("   ");
    }

    @Test
    void shouldMaskPhoneNumber_keepLastThreeDigits() {
        // given
        String phone = "+44 7911 123456";

        // when
        String maskedPhone = dataMaskingService.maskPhoneNumber(phone);

        // then
        assertThat(maskedPhone).isEqualTo("+** **** ***456");
    }

    @Test
    void shouldMaskPhoneNumber_simpleFormat() {
        // given
        String phone = "1234567890";

        // when
        String maskedPhone = dataMaskingService.maskPhoneNumber(phone);

        // then
        assertThat(maskedPhone).isEqualTo("*******890");
    }

    @Test
    void shouldMaskPhoneNumber_shortNumber() {
        // given
        String phone = "12";

        // when
        String maskedPhone = dataMaskingService.maskPhoneNumber(phone);

        // then
        assertThat(maskedPhone).isEqualTo("**");
    }

    @Test
    void shouldMaskPhoneNumber_withSpacesAndDashes() {
        // given
        String phone = "123-456-7890";

        // when
        String maskedPhone = dataMaskingService.maskPhoneNumber(phone);

        // then
        assertThat(maskedPhone).isEqualTo("***-***-*890");
    }

    @Test
    void shouldMaskPhoneNumber_handleNullAndEmpty() {
        assertThat(dataMaskingService.maskPhoneNumber(null)).isNull();
        assertThat(dataMaskingService.maskPhoneNumber("")).isEqualTo("");
        assertThat(dataMaskingService.maskPhoneNumber("   ")).isEqualTo("   ");
    }

    @Test
    void shouldMaskEmail_keepFirstCharacterAndDomain() {
        // given
        String email = "jonathansmith@example.com";

        // when
        String maskedEmail = dataMaskingService.maskEmail(email);

        // then
        assertThat(maskedEmail).isEqualTo("j************@example.com");
    }

    @Test
    void shouldMaskEmail_shortLocalPart() {
        // given
        String email = "j@example.com";

        // when
        String maskedEmail = dataMaskingService.maskEmail(email);

        // then
        assertThat(maskedEmail).isEqualTo("j@example.com");
    }

    @Test
    void shouldMaskEmail_invalidFormat() {
        // given
        String email = "notanemail";

        // when
        String maskedEmail = dataMaskingService.maskEmail(email);

        // then
        assertThat(maskedEmail).isEqualTo("n*********");
    }

    @Test
    void shouldMaskEmail_handleNullAndEmpty() {
        assertThat(dataMaskingService.maskEmail(null)).isNull();
        assertThat(dataMaskingService.maskEmail("")).isEqualTo("");
        assertThat(dataMaskingService.maskEmail("   ")).isEqualTo("   ");
    }

    @Test
    void shouldMaskJsonData_allFields() {
        // given
        String jsonData = """
                {
                    "name": "Jonathan Smith",
                    "phone": "+44 7911 123456",
                    "email": "jonathansmith@example.com"
                }
                """;

        // when
        String maskedJson = dataMaskingService.maskJsonData(jsonData);

        // then
        assertThat(maskedJson).contains("\"name\":\"J******* S****\"");
        assertThat(maskedJson).contains("\"phone\":\"+** **** ***456\"");
        assertThat(maskedJson).contains("\"email\":\"j************@example.com\"");
    }

    @Test
    void shouldMaskJsonData_nestedObject() {
        // given
        String jsonData = """
                {
                    "user": {
                        "name": "John Doe",
                        "email": "john@example.com"
                    },
                    "contact": {
                        "phone": "123-456-7890"
                    }
                }
                """;

        // when
        String maskedJson = dataMaskingService.maskJsonData(jsonData);

        // then
        assertThat(maskedJson).contains("\"name\":\"J*** D**\"");
        assertThat(maskedJson).contains("\"email\":\"j***@example.com\"");
        assertThat(maskedJson).contains("\"phone\":\"***-***-*890\"");
    }

    @Test
    void shouldMaskJsonData_arrayOfObjects() {
        // given
        String jsonData = """
                {
                    "users": [
                        {
                            "name": "John Doe",
                            "email": "john@example.com"
                        },
                        {
                            "name": "Jane Smith",
                            "email": "jane@example.com"
                        }
                    ]
                }
                """;

        // when
        String maskedJson = dataMaskingService.maskJsonData(jsonData);

        // then
        assertThat(maskedJson).contains("\"name\":\"J*** D**\"");
        assertThat(maskedJson).contains("\"name\":\"J*** S****\"");
        assertThat(maskedJson).contains("\"email\":\"j***@example.com\"");
        assertThat(maskedJson).contains("\"email\":\"j***@example.com\"");
    }

    @Test
    void shouldMaskJsonData_ignoreNonSensitiveFields() {
        // given
        String jsonData = """
                {
                    "name": "John Doe",
                    "age": 30,
                    "address": "123 Main St",
                    "phone": "123-456-7890"
                }
                """;

        // when
        String maskedJson = dataMaskingService.maskJsonData(jsonData);

        // then
        assertThat(maskedJson).contains("\"name\":\"J*** D**\"");
        assertThat(maskedJson).contains("\"phone\":\"***-***-*890\"");
        assertThat(maskedJson).contains("\"age\":30");
        assertThat(maskedJson).contains("\"address\":\"123 Main St\"");
    }

    @Test
    void shouldMaskJsonData_handleInvalidJson() {
        // given
        String invalidJson = "{ invalid json }";

        // when
        String result = dataMaskingService.maskJsonData(invalidJson);

        // then
        assertThat(result).isEqualTo(invalidJson);
    }

    @Test
    void shouldCheckMaskingEnabled_defaultFields() {
        assertThat(dataMaskingService.isMaskingEnabledForField("name")).isTrue();
        assertThat(dataMaskingService.isMaskingEnabledForField("phone")).isTrue();
        assertThat(dataMaskingService.isMaskingEnabledForField("phoneNumber")).isTrue();
        assertThat(dataMaskingService.isMaskingEnabledForField("phone_number")).isTrue();
        assertThat(dataMaskingService.isMaskingEnabledForField("email")).isTrue();
        assertThat(dataMaskingService.isMaskingEnabledForField("emailAddress")).isTrue();
        assertThat(dataMaskingService.isMaskingEnabledForField("email_address")).isTrue();
        assertThat(dataMaskingService.isMaskingEnabledForField("age")).isFalse();
        assertThat(dataMaskingService.isMaskingEnabledForField("address")).isFalse();
    }

    @Test
    void shouldCheckMaskingEnabled_customFields() {
        // given
        var customService = new DataMaskingServiceImpl(monitor, true, "customField", "another");

        // then
        assertThat(customService.isMaskingEnabledForField("customField")).isTrue();
        assertThat(customService.isMaskingEnabledForField("customfield")).isTrue(); // case insensitive
        assertThat(customService.isMaskingEnabledForField("another")).isTrue();
        assertThat(customService.isMaskingEnabledForField("name")).isFalse();
    }

    @Test
    void shouldCheckMaskingEnabled_disabled() {
        // given
        var disabledService = new DataMaskingServiceImpl(monitor, false);

        // then
        assertThat(disabledService.isMaskingEnabledForField("name")).isFalse();
        assertThat(disabledService.isMaskingEnabledForField("email")).isFalse();
    }
}
