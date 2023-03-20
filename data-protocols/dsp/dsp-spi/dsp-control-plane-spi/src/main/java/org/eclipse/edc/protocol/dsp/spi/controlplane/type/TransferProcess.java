package org.eclipse.edc.protocol.dsp.spi.controlplane.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;

@JsonTypeName("dspace:TransferProcess")
@JsonDeserialize(builder = TransferProcess.Builder.class)
public class TransferProcess {

    private String correlationId;

    private String state;

    @JsonProperty("dspace:correlationId")
    public String getCorrelationId(){
        return correlationId;
    }

    @JsonProperty("dspace:state")
    public String getState(){
        return state;
    }

    public static class Builder{
        TransferProcess transferProcess;

        Builder(){
            transferProcess = new TransferProcess();
        }

        @JsonCreator
        public static TransferProcess.Builder newInstance(){
            return new TransferProcess.Builder();
        }

        public TransferProcess.Builder correlationId(String correlationId) {
            transferProcess.correlationId = correlationId;
            return this;
        }

        public TransferProcess.Builder state(String state){
            transferProcess.state = state;
            return this;
        }

        public TransferProcess build() {
            Objects.requireNonNull(transferProcess.correlationId,"The correlationId must be specified");
            Objects.requireNonNull(transferProcess.state,"The state must be specified");

            return transferProcess;
        }

    }
}
