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

package org.eclipse.edc.connector.dataplane.spi.pipeline;

import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure.Reason.GENERAL_ERROR;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure.Reason.NOT_AUTHORIZED;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure.Reason.NOT_FOUND;

/**
 * Specialized {@link Result} class to indicate the success or failure opening a source stream.
 *
 * @param <T> The type of content
 */
public class StreamResult<T> extends AbstractResult<T, StreamFailure, StreamResult<T>> {

    protected StreamResult(T content, StreamFailure failure) {
        super(content, failure);
    }

    public static <T> StreamResult<T> success(T content) {
        return new StreamResult<>(content, null);
    }

    public static <T> StreamResult<T> notFound() {
        return new StreamResult<>(null, new StreamFailure(emptyList(), NOT_FOUND));
    }

    public static <T> StreamResult<T> notAuthorized() {
        return new StreamResult<>(null, new StreamFailure(emptyList(), NOT_AUTHORIZED));
    }

    public static <T> StreamResult<T> error(String message) {
        return new StreamResult<>(null, new StreamFailure(List.of(message), GENERAL_ERROR));
    }

    public static <T> StreamResult<T> success() {
        return StreamResult.success(null);
    }

    public static <T> StreamResult<T> failure(StreamFailure failure) {
        return new StreamResult<>(null, failure);
    }

    public StreamFailure.Reason reason() {
        return getFailure().getReason();
    }
}
