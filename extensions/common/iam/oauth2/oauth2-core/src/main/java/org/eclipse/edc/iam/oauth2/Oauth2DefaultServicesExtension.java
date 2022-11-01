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

package org.eclipse.edc.iam.oauth2;

import org.eclipse.edc.iam.oauth2.spi.CredentialsRequestAdditionalParametersProvider;
import org.eclipse.edc.iam.oauth2.spi.NoopCredentialsRequestAdditionalParametersProvider;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

/**
 * Provides default service implementations for fallback
 * Omitted {@link org.eclipse.edc.runtime.metamodel.annotation.Extension} since this module already contains {@link Oauth2Extension}
 */
public class Oauth2DefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "OAuth2 Core Default Services";

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public CredentialsRequestAdditionalParametersProvider credentialsRequestAdditionalParametersProvider() {
        return new NoopCredentialsRequestAdditionalParametersProvider();
    }

}
