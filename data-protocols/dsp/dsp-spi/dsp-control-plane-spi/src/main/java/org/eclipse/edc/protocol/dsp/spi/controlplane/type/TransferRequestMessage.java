package org.eclipse.edc.protocol.dsp.spi.controlplane.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

@JsonTypeName("dspace:TransferRequestMessage")
@JsonDeserialize(builder = TransferRequestMessage.Builder.class)
public class TransferRequestMessage {

    private String agreementId;

    private String format;

    private String dataAddress;

    private String callbackAddress;

    @JsonProperty("dspace:agreementId")
    public String getAgreementId() {
        return agreementId;
    }

    @JsonProperty("dct:format")
    public String getFormat() {
        return format;
    }

    @JsonProperty("dspace:dataAddress")
    public String getDataAddress() {
        return dataAddress;
    }

    @JsonProperty("dspace:callbackAddress")
    public String getCallbackAddress() {
        return callbackAddress;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder{
        TransferRequestMessage transferRequestMessage;

        Builder(){transferRequestMessage = new TransferRequestMessage();}

        @JsonCreator
        public static TransferRequestMessage.Builder newInstance() { return new TransferRequestMessage.Builder();}

        public TransferRequestMessage.Builder agreementId(String agreementId){
            transferRequestMessage.agreementId = agreementId;
            return this;
        }

        public TransferRequestMessage.Builder format(String format){
            transferRequestMessage.format= format;
            return this;
        }

        public TransferRequestMessage.Builder dataAddress(String dataAddress){
            transferRequestMessage.dataAddress = dataAddress;
            return this;
        }

        public TransferRequestMessage.Builder callbackAddress(String callbackAddress){
            transferRequestMessage.callbackAddress = callbackAddress;
            return this;
        }

        public TransferRequestMessage build(){
            Objects.requireNonNull(transferRequestMessage.agreementId,"The agreementId must be specified");
            Objects.requireNonNull(transferRequestMessage.format,"The format must be specified");
            Objects.requireNonNull(transferRequestMessage.dataAddress,"The dataAddess must be specified");
            Objects.requireNonNull(transferRequestMessage.callbackAddress,"The callbackAddress must be specified");


            return transferRequestMessage;
        }
    }
}
