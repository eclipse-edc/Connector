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

package org.eclipse.dataspaceconnector.api.result;

import org.eclipse.dataspaceconnector.spi.result.AbstractResult;

import java.util.List;

import static org.eclipse.dataspaceconnector.api.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.dataspaceconnector.api.result.ServiceFailure.Reason.NOT_FOUND;

public class ServiceResult<T> extends AbstractResult<T, ServiceFailure> {

    public static <T> ServiceResult<T> success(T content) {
        return new ServiceResult<>(content, null);
    }

    public static <T> ServiceResult<T> conflict(String message) {
        return new ServiceResult<>(null, new ServiceFailure(List.of(message), CONFLICT));
    }

    public static <T> ServiceResult<T> notFound(String message) {
        return new ServiceResult<>(null, new ServiceFailure(List.of(message), NOT_FOUND));
    }

    protected ServiceResult(T content, ServiceFailure failure) {
        super(content, failure);
    }

    public ServiceFailure.Reason reason() {
        return getFailure().getReason();
    }
}
