/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.dataloader.cli;

import org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import picocli.CommandLine;

public class DataLoaderRuntime extends BaseRuntime {

    private static ServiceExtensionContext context;

    public static void main(String[] args) {
        var runtime = new DataLoaderRuntime();
        runtime.boot();
        var assetSink = context.getService(AssetLoader.class, true);
        var contractsSink = context.getService(ContractDefinitionLoader.class, true);

        if (assetSink == null) {
            context.getMonitor().severe("No AssetLoader was configured - loading assets will not work. Please check your build file!");
            System.exit(-1);
        }
        if (contractsSink == null) {
            context.getMonitor().severe("No ContractDefinition loader was configured - loading ContractDefinitions will not work. Please check your build file!");
            System.exit(-1);
        }

        var exitCode = new CommandLine(new LoadCommand(context.getTypeManager().getMapper(), assetSink, contractsSink)).execute(args);
        System.exit(exitCode);
    }

    @Override
    protected void initializeContext(ServiceExtensionContext context) {
        super.initializeContext(context);
        DataLoaderRuntime.context = context;
    }

    @Override
    protected String getRuntimeName(ServiceExtensionContext context) {
        return "DataLoader Runtime";
    }

}
