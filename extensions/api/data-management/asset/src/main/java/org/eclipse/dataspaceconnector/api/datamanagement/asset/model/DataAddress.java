package org.eclipse.dataspaceconnector.api.datamanagement.asset.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;

@JsonDeserialize(builder = DataAddress.Builder.class)
public class DataAddress {

    private Map<String, Object> properties;

    private DataAddress(){
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder{

        private final DataAddress dataAddress;

        private Builder(){
            dataAddress = new DataAddress();
        }

        @JsonCreator
        public static Builder newInstance(){
            return new Builder();
        }

        public Builder properties(Map<String,Object> properties) {
            dataAddress.properties = properties;
            return this;
        }

        public DataAddress build(){
            return  dataAddress;
        }

    }
}
