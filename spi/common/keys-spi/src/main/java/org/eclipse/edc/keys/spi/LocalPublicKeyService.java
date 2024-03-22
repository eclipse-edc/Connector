/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.keys.spi;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Specialized interface of {@link PublicKeyResolver} which can be injected as component and available globally in the runtime.
 * for resolving public keys configured in the runtime (vault, config, etc.)
 */
@ExtensionPoint
@FunctionalInterface
public interface LocalPublicKeyService extends PublicKeyResolver {

}
