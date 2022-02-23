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
 *
 */
package org.eclipse.dataspaceconnector.contract.offer;

import org.eclipse.dataspaceconnector.contract.common.ContractId;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Implementation of the {@link ContractOfferService}.
 */
public class ContractOfferServiceImpl implements ContractOfferService {
    private final ParticipantAgentService agentService;
    private final ContractDefinitionService definitionService;
    private final AssetIndex assetIndex;

    public ContractOfferServiceImpl(ParticipantAgentService agentService, ContractDefinitionService definitionService, AssetIndex assetIndex) {
        this.agentService = Objects.requireNonNull(agentService, "ParticipantAgentService must not be null");
        this.definitionService = Objects.requireNonNull(definitionService, "ContractDefinitionService must not be null");
        this.assetIndex = Objects.requireNonNull(assetIndex, "AssetIndex must not be null");
    }

    @Override
    @NotNull
    public Stream<ContractOffer> queryContractOffers(ContractOfferQuery query) {
        var agent = agentService.createFor(query.getClaimToken());
        var definitions = definitionService.definitionsFor(agent);

        return definitions.flatMap(definition -> {
            var assets = assetIndex.queryAssets(definition.getSelectorExpression());
            return assets.map(asset -> ContractOffer.Builder.newInstance()
                    .id(ContractId.createContractId(definition.getId()))
                    .policy(createTargetedPolicy(definition.getContractPolicy(), asset.getId()))
                    .asset(asset)
                    .build());
        });
    }

    // TODO Completely solve the targeted policy problem in issue #609
    private Policy createTargetedPolicy(Policy policy, String target) {
        Policy.Builder builder = Policy.Builder.newInstance();
        builder.id(policy.getUid()); // TODO It may become an issue to generate multiple policies with same ID but different target
        builder.target(target);
        builder.type(policy.getType());
        builder.extensibleProperties(policy.getExtensibleProperties());
        builder.assigner(policy.getAssigner());
        builder.assignee(policy.getAssignee());

        if (policy.getPermissions() != null) {
            for (Permission permission : policy.getPermissions()) {
                builder.permission(createTargetedPermission(permission, target));
            }
        }

        if (policy.getObligations() != null) {
            for (Duty duty : policy.getObligations()) {
                builder.duty(createTargetedDuty(duty, target));
            }
        }

        if (policy.getProhibitions() != null) {
            for (Prohibition prohibition : policy.getProhibitions()) {
                builder.prohibition(createTargetedProhibition(prohibition, target));
            }
        }

        return builder.build();
    }

    private Permission createTargetedPermission(Permission permission, String target) {
        if (permission == null) {
            return null;
        }

        Permission.Builder builder = Permission.Builder.newInstance();
        builder.uid(permission.getUid()); // TODO It may become an issue to generate multiple permissions with same ID but different target
        builder.assignee(permission.getAssignee());
        builder.assigner(permission.getAssigner());
        builder.target(target);
        builder.action(permission.getAction());
        builder.constraints(permission.getConstraints());
        if (permission.getDuties() != null) {
            for (Duty duty : permission.getDuties()) {
                builder.duty(createTargetedDuty(duty, target));
            }
        }

        return builder.build();
    }

    private Prohibition createTargetedProhibition(Prohibition prohibition, String target) {
        if (prohibition == null) {
            return null;
        }

        Prohibition.Builder builder = Prohibition.Builder.newInstance();
        builder.assignee(prohibition.getAssignee());
        builder.assigner(prohibition.getAssigner());
        builder.target(target);
        builder.action(prohibition.getAction());
        builder.constraints(prohibition.getConstraints());

        return builder.build();
    }

    private Duty createTargetedDuty(Duty duty, String target) {
        if (duty == null) {
            return null;
        }

        Duty.Builder builder = Duty.Builder.newInstance();
        builder.uid(duty.getUid()); // TODO It may become an issue to generate multiple duties with same ID but different target
        builder.assigner(duty.getAssigner());
        builder.assignee(duty.getAssignee());
        builder.action(duty.getAction());
        builder.target(target);
        builder.parentPermission(duty.getParentPermission());
        builder.consequence(createTargetedDuty(duty.getConsequence(), target));
        builder.parentPermission(createTargetedPermission(duty.getParentPermission(), target));
        builder.constraints(duty.getConstraints());

        return builder.build();
    }
}
