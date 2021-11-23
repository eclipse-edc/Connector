package org.eclipse.dataspaceconnector.ids.api.multipart.client;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ModelClass;
import de.fraunhofer.iais.eis.ResponseMessage;

public class MultipartDescriptionResponse implements MultipartResponse<ModelClass> {

    private ResponseMessage header;

    private ModelClass payload;

    private MultipartDescriptionResponse() { }

    @Override
    public Message getHeader() {
        return header;
    }

    @Override
    public ModelClass getPayload() {
        return payload;
    }

    public static class Builder {
        private final MultipartDescriptionResponse descriptionResponse;

        private Builder() {
            this.descriptionResponse = new MultipartDescriptionResponse();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder header(ResponseMessage header) {
            this.descriptionResponse.header = header;
            return this;
        }

        public Builder payload(ModelClass payload) {
            this.descriptionResponse.payload = payload;
            return this;
        }

        public MultipartDescriptionResponse build() {
            Objects.requireNonNull(descriptionResponse.header, "header");
            Objects.requireNonNull(descriptionResponse.payload, "payload");
            return descriptionResponse;
        }
    }

}
