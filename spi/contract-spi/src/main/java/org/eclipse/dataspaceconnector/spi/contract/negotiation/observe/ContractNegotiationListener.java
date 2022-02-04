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
 * Note that the listener is not guaranteed to be called after a state change, in case
 * the application restarts. That is relevant when using a persistent contract negotiation
 * store implementation.
 */
public interface ContractNegotiationListener {
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#REQUESTING}.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void requesting(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#REQUESTED}.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void requested(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#PROVIDER_OFFERING}.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void providerOffering(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#PROVIDER_OFFERED}.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void providerOffered(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#CONSUMER_OFFERING}.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void consumerOffering(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#CONSUMER_OFFERED}.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void consumerOffered(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#CONSUMER_APPROVING}.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void consumerApproving(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#CONSUMER_APPROVED}.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void consumerApproved(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#DECLINING}.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void declining(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#DECLINED}.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void declined(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#CONFIRMING}.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void confirming(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#CONFIRMED}.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void confirmed(ContractNegotiation negotiation) {
    }
    
    /**
     * Called after a {@link ContractNegotiation} has moved to state
     * {@link ContractNegotiationStates#ERROR}.
     *
     * @param negotiation the contract negotiation whose state has changed.
     */
    default void error(ContractNegotiation negotiation) {
    }
    
}
