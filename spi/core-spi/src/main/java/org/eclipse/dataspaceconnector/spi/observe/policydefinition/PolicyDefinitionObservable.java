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

package org.eclipse.dataspaceconnector.spi.observe.policydefinition;

import org.eclipse.dataspaceconnector.spi.observe.Observable;

/**
 * Manages and invokes {@link PolicyDefinitionListener}s when a state change related to a policy definition has happened.
 */
public interface PolicyDefinitionObservable extends Observable<PolicyDefinitionListener> {

}
