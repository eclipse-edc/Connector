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
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Transformer that applies data masking to JSON strings during data exchange.
 * This transformer integrates with EDC's transformation pipeline to
 * automatically
 * mask sensitive data fields based on configuration.
 */
public class DataMaskingTransformer implements TypeTransformer<String, String> {

    private final DataMaskingService dataMaskingService;
    private final Monitor monitor;

    public DataMaskingTransformer(DataMaskingService dataMaskingService, Monitor monitor) {
        this.dataMaskingService = dataMaskingService;
        this.monitor = monitor;
    }

    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    public Class<String> getOutputType() {
        return String.class;
    }

    @Override
    public @Nullable String transform(@NotNull String input, @NotNull TransformerContext context) {
        try {
            // Check if this looks like JSON data that should be masked
            if (shouldMaskData(input)) {
                monitor.debug("Applying data masking to JSON data");
                return dataMaskingService.maskJsonData(input);
            }
            return input;
        } catch (Exception e) {
            monitor.warning("Error applying data masking, returning original data", e);
            context.reportProblem("Data masking failed: " + e.getMessage());
            return input;
        }
    }

    /**
     * Determines if the input data should be masked.
     * Currently checks if the input appears to be JSON and contains sensitive
     * fields.
     */
    private boolean shouldMaskData(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String trimmed = input.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return false; // Not JSON
        }

        // Check if it contains any of the sensitive field names
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("\"name\"") ||
                lowerInput.contains("\"phone\"") ||
                lowerInput.contains("\"email\"") ||
                lowerInput.contains("\"phonenumber\"") ||
                lowerInput.contains("\"phone_number\"") ||
                lowerInput.contains("\"emailaddress\"") ||
                lowerInput.contains("\"email_address\"");
    }
}
