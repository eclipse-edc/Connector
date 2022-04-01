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

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message;

import de.fraunhofer.iais.eis.Message;

/**
 * Interface for multipart responses.
 *
 * @param <T> the payload type of the response.
 */
public interface MultipartResponse<T> {

    /**
     * Returns the response header.
     *
     * @return the response header.
     */
    Message getHeader();

    /**
     * Returns the response payload.
     *
     * @return the response payload.
     */
    T getPayload();

}
