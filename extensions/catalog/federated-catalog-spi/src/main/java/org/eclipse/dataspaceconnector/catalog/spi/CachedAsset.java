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
public class CachedAsset {

    private static final String PROPERTY_ORIGINATOR = "asset:prop:originator";
    private static final String PROPERTY_POLICY = "asset:prop:policy";
    private final Asset wrappedAsset;

    private CachedAsset(Asset wrappedAsset) {
        this.wrappedAsset = wrappedAsset;
    }


    @JsonIgnore
    public String getOriginator() {
        Object property = wrappedAsset.getProperty(PROPERTY_ORIGINATOR);
        return property != null ? property.toString() : null;
    }

    @JsonIgnore
    public Policy getPolicy() {
        Object property = wrappedAsset.getProperty(PROPERTY_POLICY);
        return property != null ? (Policy) property : null;
    }

    public Asset getAsset() {
        return wrappedAsset;
    }

    public String getId() {
        return wrappedAsset.getId();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private Asset asset;

        private Builder() {
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

        public Builder asset(Asset asset) {
            this.asset = asset;
            return this;
        }

        public CachedAsset build() {
            return new CachedAsset(asset);
        }
    }
}
