/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.catalog;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.PolicyScope;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;

@Extension(CatalogCoreExtension.NAME)
public class CatalogCoreExtension implements ServiceExtension {

    public static final String NAME = "Catalog Core";

    @PolicyScope
    public static final String CATALOG_SCOPE = "catalog";

    @Inject
    private AssetIndex assetIndex;

    @Inject
    private PolicyDefinitionStore policyDefinitionStore;

    @Inject
    private DistributionResolver distributionResolver;

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Inject
    private ContractDefinitionStore contractDefinitionStore;

    @Inject
    private PolicyEngine policyEngine;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DatasetResolver datasetResolver() {
        var contractDefinitionResolver = new ContractDefinitionResolverImpl(contractDefinitionStore, policyEngine, policyDefinitionStore);
        return new DatasetResolverImpl(contractDefinitionResolver, assetIndex, policyDefinitionStore,
                distributionResolver, criterionOperatorRegistry);
    }

}
