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

package org.eclipse.edc.connector.controlplane.contract.spi.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.entity.PendingGuard;

/**
 * Marker interface to define a service that permits to choose whether a ContractNegotiation will be waiting for an external
 * interaction.
 */
@FunctionalInterface
@ExtensionPoint
public interface ContractNegotiationPendingGuard extends PendingGuard<ContractNegotiation> {
}
