package org.eclipse.dataspaceconnector.ids.spi.types;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

@JsonDeserialize(builder = DataCatalog.Builder.class)
public class DataCatalog {
    private final String id;
    private final List<Asset> assets;

    private DataCatalog(@NotNull String id, @NotNull List<Asset> assets) {
        this.id = Objects.requireNonNull(id);
        this.assets = Objects.requireNonNull(assets);
    }

    public String getId() {
        return id;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String id;
        private List<Asset> assets;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder assets(List<Asset> assets) {
            this.assets = assets;
            return this;
        }

        public DataCatalog build() {
            return new DataCatalog(id, assets);
        }

    }
}
