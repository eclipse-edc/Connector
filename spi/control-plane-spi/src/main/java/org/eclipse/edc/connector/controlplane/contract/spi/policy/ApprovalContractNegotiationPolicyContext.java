/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.contract.spi.policy;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.engine.spi.PolicyScope;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Policy Context for "approval.contract.negotiation" scope. The contract policy of a provider negotiation in
 * {@code REQUESTED} state is evaluated in this scope to decide if it can be approved automatically or if it needs a manual
 * approval.
 * <p>
 * The scope deliberately does not start with {@link ContractNegotiationPolicyContext#NEGOTIATION_SCOPE}, so that only the
 * constraints explicitly bound to it are evaluated.
 * <p>
 * Besides the counter-party, the context carries the negotiated {@link Asset} and the {@link ContractDefinition} that
 * offered it, so that approval rules can be specific to an asset or generic for a contract definition. They are looked
 * up lazily, only the first time they are requested, i.e. only if a function evaluated in this scope needs them.
 */
public class ApprovalContractNegotiationPolicyContext extends ContractNegotiationPolicyContext
        implements AssetPolicyContext, ContractDefinitionPolicyContext {

    @PolicyScope
    public static final String APPROVAL_SCOPE = "approval.contract.negotiation";

    private final Supplier<Asset> asset;
    private final Supplier<ContractDefinition> contractDefinition;

    /**
     * Creates the context. The asset and the contract definition are resolved lazily: each supplier is invoked at most
     * once, the first time the corresponding value is requested, so that no lookup happens when no function needs them.
     *
     * @param participantContextId the participant context id.
     * @param agent the counter-party agent.
     * @param asset supplies the negotiated asset, or null if not available.
     * @param contractDefinition supplies the contract definition that offered the asset, or null if not available.
     */
    public ApprovalContractNegotiationPolicyContext(String participantContextId, ParticipantAgent agent,
                                                    Supplier<@Nullable Asset> asset,
                                                    Supplier<@Nullable ContractDefinition> contractDefinition) {
        super(participantContextId, agent);
        this.asset = new Memoized<>(asset);
        this.contractDefinition = new Memoized<>(contractDefinition);
    }

    @Override
    public @Nullable Asset asset() {
        return asset.get();
    }

    @Override
    public @Nullable ContractDefinition contractDefinition() {
        return contractDefinition.get();
    }

    @Override
    public String scope() {
        return APPROVAL_SCOPE;
    }

    private static final class Memoized<T> implements Supplier<T> {

        private final Supplier<T> delegate;
        private boolean resolved;
        private T value;

        private Memoized(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public synchronized T get() {
            if (!resolved) {
                value = delegate.get();
                resolved = true;
            }
            return value;
        }
    }
}
