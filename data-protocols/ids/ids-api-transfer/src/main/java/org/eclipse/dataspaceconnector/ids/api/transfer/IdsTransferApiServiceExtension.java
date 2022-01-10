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

package org.eclipse.dataspaceconnector.ids.api.transfer;

import org.eclipse.dataspaceconnector.ids.spi.daps.DapsService;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;

/**
 * Implements the IDS Controller REST API for data transfer services.
 */
public class IdsTransferApiServiceExtension implements ServiceExtension {
    @Inject
    private WebService webService;
    @Inject
    private DapsService dapService;
    @Inject
    private TransferProcessManager transferManager;
    @Inject
    private AssetIndex assetIndex;
    @Inject
    private IdsPolicyService policyService;
    @Inject
    private PolicyRegistry policyRegistry;

    @Override
    public String name() {
        return "IDS Transfer API";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerControllers(context);
    }

    private void registerControllers(ServiceExtensionContext context) {

        var vault = context.getService(Vault.class);
        webService.registerController(new ArtifactRequestController(dapService, assetIndex, transferManager, policyService, policyRegistry, vault, context.getMonitor()));
    }


}
