/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.contract.spi.definition.observe;


import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.observe.Observable;

/**
 * Interface implemented by listeners registered to observe contract definition state changes via {@link Observable#registerListener}.
 * The listener must be called after the state changes are persisted.
 */
public interface ContractDefinitionListener {

    /**
     * Called after a {@link ContractDefinition} was created.
     *
     * @param contractDefinition the contractDefinition that has been created.
     */
    default void created(ContractDefinition contractDefinition) {

    }

    /**
     * Called after a {@link ContractDefinition} was deleted.
     *
     * @param contractDefinition the contractDefinition that has been deleted.
     */
    default void deleted(ContractDefinition contractDefinition) {

    }

    /**
     * Called after a {@link ContractDefinition} was updated.
     *
     * @param contractDefinition the contractDefinition that has been updated.
     */
    default void updated(ContractDefinition contractDefinition) {

    }
}
