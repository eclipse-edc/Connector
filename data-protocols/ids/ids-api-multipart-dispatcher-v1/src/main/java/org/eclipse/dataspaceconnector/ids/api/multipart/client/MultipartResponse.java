package org.eclipse.dataspaceconnector.ids.api.multipart.client;

import de.fraunhofer.iais.eis.Message;

public interface MultipartResponse<T> {

    Message getHeader();

    T getPayload();

}
