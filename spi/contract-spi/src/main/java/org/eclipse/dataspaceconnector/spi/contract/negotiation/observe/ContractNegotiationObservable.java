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
import org.eclipse.dataspaceconnector.spi.system.Feature;

/**
 * Manages and invokes {@link ContractNegotiationListener}s when an event related to a contract negotiation process is emitted.
 */
@Feature("edc:core:contract:contractnegotiation:observable")
public interface ContractNegotiationObservable extends Observable<ContractNegotiationListener> {
}
