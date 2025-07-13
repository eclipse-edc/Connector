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

package org.eclipse.edc.connector.datamasking.spi;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Service for masking sensitive data fields in data exchange flows.
 * Provides configurable masking strategies for common sensitive data types.
 */
@ExtensionPoint
public interface DataMaskingService {

    /**
     * Masks a name by keeping initials visible and masking the rest.
     * Example: "Jonathan Smith" -> "J******** S****"
     *
     * @param name the name to mask
     * @return the masked name
     */
    String maskName(String name);

    /**
     * Masks a phone number by keeping only the last 3 digits visible.
     * Example: "+44 7911 123456" -> "+44 *********456"
     *
     * @param phoneNumber the phone number to mask
     * @return the masked phone number
     */
    String maskPhoneNumber(String phoneNumber);

    /**
     * Masks an email address by keeping the first character and domain visible.
     * Example: "jonathansmith@example.com" -> "j************@example.com"
     *
     * @param email the email to mask
     * @return the masked email
     */
    String maskEmail(String email);

    /**
     * Masks a JSON object by applying masking to configured fields.
     *
     * @param jsonObject the JSON object as string
     * @return the JSON object with masked fields
     */
    String maskJsonData(String jsonObject);

    /**
     * Checks if data masking is enabled for the given field.
     *
     * @param fieldName the field name to check
     * @return true if masking is enabled for this field
     */
    boolean isMaskingEnabledForField(String fieldName);
}
