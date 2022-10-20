/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.contract.spi.negotiation;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.retry.WaitStrategy;

/**
 * Implements a wait strategy for the {@link ContractNegotiationManager}.
 * <p>
 * Implementations may choose to enforce an incremental backoff period when successive errors are encountered.
 */
@FunctionalInterface
@ExtensionPoint
public interface NegotiationWaitStrategy extends WaitStrategy {


}
