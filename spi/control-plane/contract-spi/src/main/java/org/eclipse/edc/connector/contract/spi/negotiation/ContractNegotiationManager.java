/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - added method
 *
 */

package org.eclipse.edc.connector.contract.spi.negotiation;

import org.eclipse.edc.connector.contract.spi.types.command.ContractNegotiationCommand;

/**
 * Manages contract negotiations on a consumer or provider. Subtypes define consumer- and provider-specific operations.
 * <p>
 * All operations are idempotent.
 */
public interface ContractNegotiationManager {

    void enqueueCommand(ContractNegotiationCommand command);
}
