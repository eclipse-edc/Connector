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
