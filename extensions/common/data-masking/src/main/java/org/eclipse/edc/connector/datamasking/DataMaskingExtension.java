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
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

/**
 * Extension that provides data masking capabilities for sensitive fields
 * in data exchange flows. This extension integrates with EDC's transformation
 * pipeline to automatically mask configured sensitive data fields.
 */
@Provides(DataMaskingService.class)
public class DataMaskingExtension implements ServiceExtension {

    public static final String NAME = "Data Masking Extension";

    @Configuration
    private DataMaskingConfiguration configuration;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Setting(key = "edc.data.masking.enabled", description = "Enable data masking functionality", defaultValue = "true")
    private boolean maskingEnabled;

    @Setting(key = "edc.data.masking.fields", description = "Comma-separated list of field names to mask", required = false)
    private String fieldsToMask;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        // Parse the fields to mask
        String[] fields = new String[0];
        if (fieldsToMask != null && !fieldsToMask.trim().isEmpty()) {
            fields = fieldsToMask.split(",");
            for (int i = 0; i < fields.length; i++) {
                fields[i] = fields[i].trim().toLowerCase();
            }
        }

        var dataMaskingService = new DataMaskingServiceImpl(monitor, maskingEnabled, fields);
        context.registerService(DataMaskingService.class, dataMaskingService);

        // Register the data masking transformer if masking is enabled
        if (maskingEnabled) {
            var transformer = new DataMaskingTransformer(dataMaskingService, monitor);
            transformerRegistry.register(transformer);
            monitor.info("Data Masking Transformer registered");
        }

        monitor.info("Data Masking Extension initialized. Masking enabled: " + maskingEnabled +
                ", Fields: " + (fields.length > 0 ? String.join(", ", fields) : "default"));
    }

    @Provider
    public DataMaskingService dataMaskingService(ServiceExtensionContext context) {
        return context.getService(DataMaskingService.class);
    }
}
