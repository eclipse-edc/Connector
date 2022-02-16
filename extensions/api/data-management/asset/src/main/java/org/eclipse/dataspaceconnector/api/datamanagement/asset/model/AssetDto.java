package org.eclipse.dataspaceconnector.api.datamanagement.asset.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;

@JsonDeserialize(builder = AssetDto.Builder.class)
public class AssetDto {

    private Map<String,Object> properties;

    private AssetDto() {
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder{

        private final AssetDto assetDto;

        private Builder(){

            assetDto = new AssetDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder properties(Map<String,Object> properties) {
            assetDto.properties = properties;
            return this;
        }

        public AssetDto build(){
            return assetDto;
        }
    }
}
