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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.edc.connector.datamasking.spi.DataMaskingService;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Default implementation of DataMaskingService that provides configurable
 * masking strategies for sensitive data fields.
 */
public class DataMaskingServiceImpl implements DataMaskingService {

    private static final String MASK_CHARACTER = "*";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^(.+)@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(.+)([0-9]{3})$");

    private final ObjectMapper objectMapper;
    private final Monitor monitor;
    private final Set<String> enabledFields;
    private final boolean globallyEnabled;

    public DataMaskingServiceImpl(Monitor monitor, boolean globallyEnabled, String... enabledFields) {
        this.monitor = monitor;
        this.objectMapper = new ObjectMapper();
        this.globallyEnabled = globallyEnabled;
        this.enabledFields = Arrays.stream(enabledFields)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public String maskName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }

        String trimmedName = name.trim();
        String[] parts = trimmedName.split("\\s+");
        StringBuilder maskedName = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                maskedName.append(" ");
            }

            String part = parts[i];
            if (part.length() == 1) {
                maskedName.append(part);
            } else {
                maskedName.append(part.charAt(0));
                maskedName.append(MASK_CHARACTER.repeat(part.length() - 1));
            }
        }

        return maskedName.toString();
    }

    @Override
    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return phoneNumber;
        }

        String trimmedPhone = phoneNumber.trim();

        // Extract digits from the phone number
        String digitsOnly = trimmedPhone.replaceAll("[^0-9]", "");

        if (digitsOnly.length() < 3) {
            // If less than 3 digits, mask everything
            return MASK_CHARACTER.repeat(trimmedPhone.length());
        }

        // Keep last 3 digits
        String lastThreeDigits = digitsOnly.substring(digitsOnly.length() - 3);

        // Find where the last 3 digits appear in the original string
        int lastDigitIndex = trimmedPhone.length() - 1;
        int digitsFound = 0;

        StringBuilder masked = new StringBuilder(trimmedPhone);

        // Work backwards to find and preserve the last 3 digits
        for (int i = lastDigitIndex; i >= 0 && digitsFound < 3; i--) {
            if (Character.isDigit(trimmedPhone.charAt(i))) {
                digitsFound++;
            }
        }

        // Mask all digits except the last 3
        digitsFound = 0;
        for (int i = lastDigitIndex; i >= 0; i--) {
            if (Character.isDigit(trimmedPhone.charAt(i))) {
                digitsFound++;
                if (digitsFound > 3) {
                    masked.setCharAt(i, MASK_CHARACTER.charAt(0));
                }
            }
        }

        return masked.toString();
    }

    @Override
    public String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return email;
        }

        String trimmedEmail = email.trim();
        var matcher = EMAIL_PATTERN.matcher(trimmedEmail);

        if (!matcher.matches()) {
            // If it's not a valid email format, mask everything except first character
            if (trimmedEmail.length() <= 1) {
                return trimmedEmail;
            }
            return trimmedEmail.charAt(0) + MASK_CHARACTER.repeat(trimmedEmail.length() - 1);
        }

        String localPart = matcher.group(1);
        String domainPart = matcher.group(2);

        if (localPart.length() <= 1) {
            return trimmedEmail; // Don't mask if local part is too short
        }

        String maskedLocalPart = localPart.charAt(0) + MASK_CHARACTER.repeat(localPart.length() - 1);
        return maskedLocalPart + "@" + domainPart;
    }

    @Override
    public String maskJsonData(String jsonObject) {
        if (jsonObject == null || jsonObject.trim().isEmpty()) {
            return jsonObject;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(jsonObject);
            if (rootNode.isObject()) {
                ObjectNode objectNode = (ObjectNode) rootNode;
                maskObjectNode(objectNode);
            }
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            monitor.warning("Failed to parse JSON for masking, returning original data", e);
            return jsonObject;
        }
    }

    @Override
    public boolean isMaskingEnabledForField(String fieldName) {
        if (!globallyEnabled) {
            return false;
        }

        if (enabledFields.isEmpty()) {
            // If no specific fields configured, enable for common sensitive fields
            return "name".equalsIgnoreCase(fieldName) ||
                    "phone".equalsIgnoreCase(fieldName) ||
                    "phoneNumber".equalsIgnoreCase(fieldName) ||
                    "phone_number".equalsIgnoreCase(fieldName) ||
                    "email".equalsIgnoreCase(fieldName) ||
                    "emailAddress".equalsIgnoreCase(fieldName) ||
                    "email_address".equalsIgnoreCase(fieldName);
        }

        return enabledFields.contains(fieldName.toLowerCase());
    }

    private void maskObjectNode(ObjectNode objectNode) {
        objectNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();

            if (fieldValue.isTextual() && isMaskingEnabledForField(fieldName)) {
                String originalValue = fieldValue.asText();
                String maskedValue = maskField(fieldName, originalValue);
                objectNode.put(fieldName, maskedValue);
            } else if (fieldValue.isObject()) {
                maskObjectNode((ObjectNode) fieldValue);
            } else if (fieldValue.isArray()) {
                fieldValue.forEach(arrayElement -> {
                    if (arrayElement.isObject()) {
                        maskObjectNode((ObjectNode) arrayElement);
                    }
                });
            }
        });
    }

    private String maskField(String fieldName, String value) {
        String lowerFieldName = fieldName.toLowerCase();

        if ("name".equals(lowerFieldName)) {
            return maskName(value);
        } else if ("phone".equals(lowerFieldName) || "phonenumber".equals(lowerFieldName) ||
                "phone_number".equals(lowerFieldName)) {
            return maskPhoneNumber(value);
        } else if ("email".equals(lowerFieldName) || "emailaddress".equals(lowerFieldName) ||
                "email_address".equals(lowerFieldName)) {
            return maskEmail(value);
        }

        // Default masking strategy - keep first character
        if (value.length() <= 1) {
            return value;
        }
        return value.charAt(0) + MASK_CHARACTER.repeat(value.length() - 1);
    }
}
