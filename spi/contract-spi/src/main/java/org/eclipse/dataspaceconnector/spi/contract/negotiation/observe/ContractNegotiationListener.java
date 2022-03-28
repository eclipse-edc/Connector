/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.spi.contract.negotiation.observe;

import org.eclipse.dataspaceconnector.spi.observe.Observable;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;

/**
 * Interface implemented by listeners registered to observe contract negotiation state changes
 * via {@link Observable#registerListener}.
 * <p>
 * Note that the listener is called before state changes are persisted.
 * Therefore, when using a persistent contract negotiation store implementation, it
 * is guaranteed to be called at least once.
 */
public interface ContractNegotiationListener {
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#REQUESTING}, but before the change is persisted.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void preRequesting(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#REQUESTED}, but before the change is persisted.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void preRequested(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#PROVIDER_OFFERING}, but before the change is persisted.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void preProviderOffering(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#PROVIDER_OFFERED}, but before the change is persisted.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void preProviderOffered(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#CONSUMER_OFFERING}, but before the change is persisted.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void preConsumerOffering(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#CONSUMER_OFFERED}, but before the change is persisted.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void preConsumerOffered(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#CONSUMER_APPROVING}, but before the change is persisted.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void preConsumerApproving(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#CONSUMER_APPROVED}, but before the change is persisted.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void preConsumerApproved(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#DECLINING}, but before the change is persisted.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void preDeclining(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#DECLINED}, but before the change is persisted.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void preDeclined(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#CONFIRMING}, but before the change is persisted.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void preConfirming(ContractNegotiation negotiation) {
    }

    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#CONFIRMED}, but before the change is persisted.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void preConfirmed(ContractNegotiation negotiation) {
    }

    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#ERROR}, but before the change is persisted.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void preError(ContractNegotiation negotiation) {
    }
}
