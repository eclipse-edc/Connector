package org.eclipse.dataspaceconnector.ids.api.multipart.client;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ResponseMessage;

public class MultipartDescriptionResponse<T> implements MultipartResponse<T> {

    private final ResponseMessage header;

    private final T payload;

    public MultipartDescriptionResponse(final ResponseMessage header, final T payload) {
        this.header = header;
        this.payload = payload;
    }

    @Override
    public Message getHeader() {
        return header;
    }

    @Override
    public T getPayload() {
        return payload;
    }

    //TODO builder

}
