package org.eclipse.dataspaceconnector.ids.api.multipart.client;

import de.fraunhofer.iais.eis.Message;

//TODO T extends ModelClass (later Infomodel version)
public interface MultipartResponse<T> {

    Message getHeader();

    T getPayload();

}
