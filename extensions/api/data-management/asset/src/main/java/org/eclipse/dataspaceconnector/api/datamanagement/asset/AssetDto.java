package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import java.util.Map;

public class AssetDto {

    private Map<String,Object> properties;

    public AssetDto(Map<String,Object> properties){
        this.properties = properties;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "org.eclipse.dataspaceconnector.api.datamanagement.asset.AssetDto{" +
                "properties=" + properties +
                '}';
    }
}
