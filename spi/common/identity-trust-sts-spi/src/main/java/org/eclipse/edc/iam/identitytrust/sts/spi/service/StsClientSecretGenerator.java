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

package org.eclipse.edc.iam.identitytrust.sts.spi.service;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.jetbrains.annotations.Nullable;

@ExtensionPoint
@FunctionalInterface
public interface StsClientSecretGenerator {
    /**
     * Generates a client secret as string, taking an optional argument. By default,
     *
     * @param parameters Optional generator arguments, such as a salt value
     * @return a randomly generated client secret.
     */
    String generateClientSecret(@Nullable Object parameters);
}
