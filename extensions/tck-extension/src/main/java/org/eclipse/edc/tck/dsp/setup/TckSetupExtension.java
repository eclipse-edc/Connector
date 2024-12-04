/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dsp.setup;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.tck.dsp.data.DataAssembly.createAssets;
import static org.eclipse.edc.tck.dsp.data.DataAssembly.createContractDefinitions;
import static org.eclipse.edc.tck.dsp.data.DataAssembly.createPolicyDefinitions;
import static org.eclipse.edc.tck.dsp.setup.TckSetupExtension.NAME;

/**
 * Loads customizations and seed data for the TCK.
 */
@Extension(NAME)
public class TckSetupExtension implements ServiceExtension {
    public static final String NAME = "DSP TCK Setup";

    @Inject
    private AssetIndex assetIndex;

    @Inject
    private PolicyDefinitionService policyDefinitionService;

    @Inject
    private ContractDefinitionService contractDefinitionService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void prepare() {
        createAssets().forEach(asset -> assetIndex.create(asset));
        createPolicyDefinitions().forEach(definition -> policyDefinitionService.create(definition));
        createContractDefinitions().forEach(definition -> contractDefinitionService.create(definition));
    }

}
