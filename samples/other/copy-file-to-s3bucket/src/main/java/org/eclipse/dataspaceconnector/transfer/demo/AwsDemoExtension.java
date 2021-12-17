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
package org.eclipse.dataspaceconnector.transfer.demo;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.transfer.inline.core.InlineDataFlowController;
import org.eclipse.dataspaceconnector.transfer.inline.spi.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.writer.s3.S3BucketWriter;

import java.time.temporal.ChronoUnit;

public class AwsDemoExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);
        var dataFlowManager = context.getService(DataFlowManager.class);
        var dataAddressResolver = context.getService(DataAddressResolver.class);

        var dataOperatorRegistry = context.getService(DataOperatorRegistry.class);
        dataOperatorRegistry.registerReader(new DemoDataReader());

        RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                .withBackoff(500, 5000, ChronoUnit.MILLIS)
                .withMaxRetries(3);
        dataOperatorRegistry.registerWriter(new S3BucketWriter(context.getMonitor(), context.getTypeManager(), retryPolicy));

        DataFlowController dataFlowController = new InlineDataFlowController(vault, context.getMonitor(), dataOperatorRegistry, dataAddressResolver);
        dataFlowManager.register(dataFlowController);
    }
}
