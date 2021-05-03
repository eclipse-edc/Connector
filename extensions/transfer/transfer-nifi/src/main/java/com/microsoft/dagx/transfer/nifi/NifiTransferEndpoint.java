package com.microsoft.dagx.transfer.nifi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.microsoft.dagx.spi.types.domain.Polymorphic;

@JsonTypeName("dagx:nifitransferendpoint")
public abstract class NifiTransferEndpoint implements Polymorphic {
    @JsonProperty("key")
    private String key;
    @JsonProperty("type")
    private String type;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public  String getType(){
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
