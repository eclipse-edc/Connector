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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - minor modifications
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.negotiation;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.entity.StateEntityManager;

/**
 * Manages contract negotiations on the consumer participant.
 * <p>
 * All operations are idempotent.
 */
@ExtensionPoint
public interface ConsumerContractNegotiationManager extends StateEntityManager {

}
