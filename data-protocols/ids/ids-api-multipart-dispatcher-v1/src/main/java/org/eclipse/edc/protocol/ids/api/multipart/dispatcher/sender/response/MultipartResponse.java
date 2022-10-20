/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - Initial implementation
 *
 */

package org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response;

import de.fraunhofer.iais.eis.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a multipart response received from another connector. A multipart response consists
 * of a {@link Message} as header and an optional payload.
 *
 * @param <T> the payload type of the response.
 */
public class MultipartResponse<T> {

    private final Message header;

    @Nullable
    private final T payload;

    /**
     * Constructs a new MultipartResponse.
     *
     * @param header the response header.
     * @param payload the response payload.
     */
    public MultipartResponse(@NotNull Message header, @Nullable T payload) {
        this.header = header;
        this.payload = payload;
    }

    /**
     * Returns the response header.
     *
     * @return the response header.
     */
    public @NotNull Message getHeader() {
        return header;
    }

    /**
     * Returns the response payload. May be null.
     *
     * @return the response payload.
     */
    public @Nullable T getPayload() {
        return payload;
    }

}
