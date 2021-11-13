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
 *       Microsoft Corporation - Refactoring
 */
package org.eclipse.dataspaceconnector.demo.contract.offer;

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferFramework;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression.SELECT_ALL;

/**
 * Creates free of use contract offers for all assets.
 */
public class PublicContractOfferFramework implements ContractOfferFramework {

    @Override
    @NotNull
    public Stream<ContractDefinition> definitionsFor(ParticipantAgent agent) {
        return Stream.of(new ContractDefinition(createPolicy(), SELECT_ALL));
    }

    @NotNull
    private Policy createPolicy() {
        var action = Action.Builder.newInstance().type("USE").build();
        var rule = Permission.Builder.newInstance().action(action).constraints(emptyList()).build();
        return Policy.Builder.newInstance().type(PolicyType.CONTRACT).permissions(List.of(rule)).build();
    }
}
