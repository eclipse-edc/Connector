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

import org.eclipse.edc.connector.api.sts.model.StsTokenErrorResponse;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Custom exception and a mapper {@link StsTokenExceptionMapper} in order to be compliant with the OAuth2 <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-5.2">Error Response</a>
 * The {@link ServiceFailure} is translated to a {@link StsTokenErrorResponse} in the mapper.
 */
public class StsTokenException extends EdcException {

    private final ServiceFailure serviceFailure;
    private final String clientId;

    public StsTokenException(ServiceFailure serviceFailure, String clientId) {
        super(serviceFailure.getFailureDetail());
        this.serviceFailure = serviceFailure;
        this.clientId = clientId;
    }

    public static Function<ServiceFailure, StsTokenException> tokenException(@Nullable String clientId) {
        return (serviceFailure -> new StsTokenException(serviceFailure, clientId));
    }

    public static Function<ValidationFailure, StsTokenException> validationException(@Nullable String clientId) {
        return (failure -> new StsTokenException(new ServiceFailure(failure.getMessages(), ServiceFailure.Reason.BAD_REQUEST), clientId));
    }

    public ServiceFailure getServiceFailure() {
        return serviceFailure;
    }

    public String getClientId() {
        return clientId;
    }
}
