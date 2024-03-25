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

package org.eclipse.edc.iam.did.spi.resolution;

import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Specialized version of {@link PublicKeyResolver} which can be injected in the runtime
 * as dependency, and specific to public keys resolvable with did urls.
 */
@ExtensionPoint
public interface DidPublicKeyResolver extends PublicKeyResolver {
}
