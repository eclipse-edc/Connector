/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.demo.contracts;

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFrameworkQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferTemplate;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Collections;
import java.util.stream.Stream;

/**
 * Creates free of use contract offers for all assets.
 */
public class PublicContractOfferFramework implements ContractOfferFramework {

    @Override
    public Stream<ContractOfferTemplate> queryTemplates(ContractOfferFrameworkQuery query) {
        return Stream.of(FixtureContractOfferTemplate.INSTANCE);
    }

    enum FixtureContractOfferTemplate implements ContractOfferTemplate {
        INSTANCE;

        @Override
        public Stream<org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer> getTemplatedOffers(Stream<Asset> assets) {
            return assets.map(this::createContractOffer);
        }

        @Override
        public AssetSelectorExpression getSelectorExpression() {
            return AssetSelectorExpression.SELECT_ALL;
        }

        private org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer createContractOffer(Asset asset) {
            org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer.Builder builder = org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer.Builder.newInstance();

            Action action = Action.Builder.newInstance()
                    .type("USE")
                    .build();

            Policy.Builder policyBuilder = Policy.Builder.newInstance()
                    .type(PolicyType.CONTRACT);

            Permission rule = Permission.Builder.newInstance()
                    .action(action)
                    .constraints(Collections.emptyList())
                    .build();
            policyBuilder.permissions(Collections.singletonList(rule));

            Policy policy = policyBuilder.build();

            return builder
                    .assets(Collections.singletonList(asset))
                    .policy(policy)
                    .build();
        }
    }
}
