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

package org.eclipse.dataspaceconnector.iam.oauth2.spi;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.dataspaceconnector.spi.iam.TokenParameters;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Permits to add custom form parameters for oauth2 client credentials request
 */
@FunctionalInterface
@ExtensionPoint
public interface CredentialsRequestAdditionalParametersProvider {

    /**
     * Provides additional form parameters on every credential request
     *
     * @param parameters token parameters
     * @return a map of parameters
     */
    @NotNull
    Map<String, String> provide(TokenParameters parameters);

}
