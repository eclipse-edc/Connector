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

package org.eclipse.edc.identitytrust;

/**
 * An interface for identity resolution, which is useful when multiple identities need to be correlated.
 * This is a temporary workaround, because currently, the {@code aud} claim of tokens is always set to the
 * counter-party's DSP callback URL, which may not suit all needs.
 * <p>
 * todo: remove once the decoupling of DSP URL and identity is done!
 */
@FunctionalInterface
public interface AudienceResolver {
    String resolve(String identity);
}
