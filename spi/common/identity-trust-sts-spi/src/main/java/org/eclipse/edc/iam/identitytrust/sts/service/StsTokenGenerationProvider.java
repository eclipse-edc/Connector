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

package org.eclipse.edc.iam.identitytrust.sts.service;

import org.eclipse.edc.iam.identitytrust.sts.model.StsClient;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Functional interface for creating a {@link TokenGenerationService} for an {@link StsTokenGenerationProvider}
 * Each client should have its own {@link TokenGenerationService}, since it's bounded to a single private key.
 */
@ExtensionPoint
@FunctionalInterface
public interface StsTokenGenerationProvider {

    /**
     * Returns a {@link TokenGenerationService} for the input {@link StsClient}
     *
     * @param client The {@link StsClient}
     * @return The {@link TokenGenerationService}
     */
    TokenGenerationService tokenGeneratorFor(StsClient client);
}
