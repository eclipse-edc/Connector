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

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.jetbrains.annotations.Nullable;

/**
 * This container object serves as request DTO for creating {@link StsAccount} entries.
 *
 * @param account      The STS Account to be created
 * @param clientSecret If present, this value will be used as {@code client_secret} for the account. If null, a client secret
 *                     will be generated.
 */
public record StsAccountCreation(StsAccount account, @Nullable String clientSecret) {
}
