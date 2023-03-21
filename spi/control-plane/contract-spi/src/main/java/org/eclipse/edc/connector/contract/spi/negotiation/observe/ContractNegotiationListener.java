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

package org.eclipse.edc.connector.contract.spi.negotiation.observe;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.observe.Observable;

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
     * Called after a {@link ContractNegotiation} was initiated.
     *
     * @param negotiation the contract negotiation that has been initiated.
     */
    default void initiated(ContractNegotiation negotiation) {

    }

    /**
     * Called after a {@link ContractNegotiation} was requested.
     *
     * @param negotiation the contract negotiation that has been requested.
     */
    default void consumerRequested(ContractNegotiation negotiation) {

    }

    /**
     * Called after a {@link ContractNegotiation} was offered.
     *
     * @param negotiation the contract negotiation that has been offered.
     */
    default void providerOffered(ContractNegotiation negotiation) {

    }

    /**
     * Called after a {@link ContractNegotiation} was approved.
     *
     * @param negotiation the contract negotiation that has been approved.
     */
    default void consumerAgreed(ContractNegotiation negotiation) {

    }

    /**
     * Called after a {@link ContractNegotiation} was terminated.
     *
     * @param negotiation the contract negotiation that has been terminated.
     */
    default void terminated(ContractNegotiation negotiation) {

    }

    /**
     * Called after a {@link ContractNegotiation} was declined.
     *
     * @param negotiation the contract negotiation that has been declined.
     * @deprecated please use {@link #terminated(ContractNegotiation)}
     */
    @Deprecated(since = "milestone9")
    default void declined(ContractNegotiation negotiation) {
        terminated(negotiation);
    }

    /**
     * Called after a {@link ContractNegotiation} failed.
     *
     * @param negotiation the contract negotiation that failed.
     * @deprecated please use {@link #terminated(ContractNegotiation)}
     */
    @Deprecated(since = "milestone9")
    default void failed(ContractNegotiation negotiation) {
        terminated(negotiation);
    }

    /**
     * Called after a {@link ContractNegotiation} was agreed by the provider.
     *
     * @param negotiation the contract negotiation that has been confirmed.
     */
    default void providerAgreed(ContractNegotiation negotiation) {

    }

    /**
     * Called after a {@link ContractNegotiation} was verified by the consumer.
     *
     * @param negotiation the contract negotiation that has been verified.
     */
    default void consumerVerified(ContractNegotiation negotiation) {

    }

    /**
     * Called after a {@link ContractNegotiation} was finalized by the provider.
     *
     * @param negotiation the contract negotiation that has been finalized.
     */
    default void providerFinalized(ContractNegotiation negotiation) {

    }
}
