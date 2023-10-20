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

package org.eclipse.edc.connector.api.sts.exception;

import org.eclipse.edc.service.spi.result.ServiceFailure;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class StsTokenException extends EdcException {

    private final ServiceFailure serviceFailure;
    private final String clientId;

    public StsTokenException(ServiceFailure serviceFailure, String clientId) {
        super(serviceFailure.getFailureDetail());
        this.serviceFailure = serviceFailure;
        this.clientId = clientId;
    }

    public static Function<ServiceFailure, StsTokenException> tokenExceptionFunction(@Nullable String clientId) {
        return (serviceFailure -> new StsTokenException(serviceFailure, clientId));
    }

    public ServiceFailure getServiceFailure() {
        return serviceFailure;
    }

    public String getClientId() {
        return clientId;
    }
}
