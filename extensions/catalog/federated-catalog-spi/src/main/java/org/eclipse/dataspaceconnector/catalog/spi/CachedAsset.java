package org.eclipse.dataspaceconnector.catalog.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

/**
 * This is a wrapper class for the {@link Asset} object, which has typed accessors for additional properties, namely a  {@link Policy} and the "originator",
 * which is the name of the connector that the asset comes from.
 */
@JsonTypeName()
@JsonDeserialize(builder = CachedAsset.Builder.class)
public class CachedAsset extends Asset {

    public static final String PROPERTY_ORIGINATOR = "asset:prop:originator";
    private static final String PROPERTY_POLICY = "asset:prop:policy";

    private CachedAsset() {
    }


    @JsonIgnore
    public String getOriginator() {
        Object property = getProperty(PROPERTY_ORIGINATOR);
        return property != null ? property.toString() : null;
    }

    @JsonIgnore
    public Policy getPolicy() {
        Object property = getProperty(PROPERTY_POLICY);
        return property != null ? (Policy) property : null;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends Asset.Builder<Builder> {

        private Builder() {
            super(new CachedAsset());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder originator(String originator) {
            asset.getProperties().put(PROPERTY_ORIGINATOR, originator);
            return this;
        }

        public Builder policy(Policy policy) {
            asset.getProperties().put(PROPERTY_POLICY, policy);
            return this;
        }

        public Builder copyFrom(Asset otherAsset) {
            otherAsset.getProperties().forEach((k, v) -> asset.getProperties().put(k, v));
            return this;
        }

        @Override
        public CachedAsset build() {
            return (CachedAsset) asset;
        }
    }
}
