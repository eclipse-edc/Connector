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

package org.eclipse.edc.api.iam.identitytrust.sts.accounts.model;

import org.jetbrains.annotations.Nullable;

/**
 * Container object to update the {@code client_secret} of an STS Account.
 *
 * @param newAlias  the alias under which the client secret should be stored in the {@link org.eclipse.edc.spi.security.Vault}
 * @param newSecret the new client_secret. If null, one will be generated.
 */
public record UpdateClientSecret(String newAlias, @Nullable String newSecret) {
}
