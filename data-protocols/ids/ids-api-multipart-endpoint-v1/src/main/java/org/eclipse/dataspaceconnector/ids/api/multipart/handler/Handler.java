/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Handler {

    /**
     * Examines whether the current handler instance is applicable handling a certain {@link MultipartRequest}.
     *
     * @param multipartRequest from another connector
     * @return true if the handler can handle the request
     */
    boolean canHandle(@NotNull MultipartRequest multipartRequest);

    /**
     * Handles the given {@link MultipartRequest}.
     *
     * @param multipartRequest from another connector
     * @return {@link MultipartResponse} or null, if the request cannot be handled (e.g. when content missing)
     */
    @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull ClaimToken claimToken);
}
