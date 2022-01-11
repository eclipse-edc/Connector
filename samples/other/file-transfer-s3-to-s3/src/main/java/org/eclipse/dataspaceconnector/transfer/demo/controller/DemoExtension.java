/*
 *  Copyright (c) 2021 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Siemens AG - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.transfer.inline.core.InlineDataFlowController;
import org.eclipse.dataspaceconnector.transfer.inline.spi.DataOperatorRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class DemoExtension implements ServiceExtension {

    public static final String USE_EU_POLICY = "use-eu";
    private Monitor monitor;
    private ServiceExtensionContext context;

    @Override
    public String name() {
        return "AWS Demo";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.context = context;
        monitor = context.getMonitor();

        var dataFlowMgr = context.getService(DataFlowManager.class);
        var vault = context.getService(Vault.class);

        var dataAddressResolver = context.getService(DataAddressResolver.class);

        var dataOperatorRegistry = context.getService(DataOperatorRegistry.class);
        dataOperatorRegistry.registerStreamPublisher(new S3toS3DataStreamer(context.getMonitor(), vault, context.getTypeManager(), dataAddressResolver));

        dataFlowMgr.register(new InlineDataFlowController(vault, context.getMonitor(), dataOperatorRegistry, dataAddressResolver));
    }

    @Override
    public void start() {
        loadDataEntries();
    }

    private void loadDataEntries() {

        var objectMapper = context.getTypeManager().getMapper();

        var source = Paths.get("provider-artifacts.json");

        try {
            if (Files.exists(source)) {
                File sourceFile = source.toFile();
                List<Asset> assets = objectMapper.readValue(sourceFile, new TypeReference<>() {
                });
                // TODO: we miss an example of the provider-artifacts.json to adapt for assetIndex storing
            }

        } catch (IOException e) {
            monitor.severe("Error loading data entries", e);
        }
    }
}
