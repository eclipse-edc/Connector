/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dsp.setup;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.tck.dsp.data.DataSeed.createAssets;
import static org.eclipse.edc.tck.dsp.data.DataSeed.createContractDefinitions;
import static org.eclipse.edc.tck.dsp.data.DataSeed.createContractNegotiations;
import static org.eclipse.edc.tck.dsp.data.DataSeed.createPolicyDefinitions;
import static org.eclipse.edc.tck.dsp.setup.TckSetupExtension.NAME;

/**
 * Loads customizations and seed data for the TCK.
 */
@Extension(NAME)
public class TckSetupExtension implements ServiceExtension {
    public static final String NAME = "DSP TCK Setup";


    @Setting(description = "Configures the participant context id for the tck suite runtime", key = "edc.participant.context.id", defaultValue = "participantContextId")
    public String participantContextId;

    @Inject
    private AssetIndex assetIndex;

    @Inject
    private PolicyDefinitionService policyDefinitionService;

    @Inject
    private ContractDefinitionService contractDefinitionService;

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void prepare() {
        createAssets(participantContextId).forEach(asset -> assetIndex.create(asset));
        createPolicyDefinitions(participantContextId).forEach(definition -> policyDefinitionService.create(definition));
        createContractDefinitions(participantContextId).forEach(definition -> contractDefinitionService.create(definition));
        createContractNegotiations(participantContextId).forEach(negotiation -> contractNegotiationStore.save(negotiation));
    }


}
