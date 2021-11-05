package org.eclipse.dataspaceconnector.spi.asset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.pagination.Cursor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

public class AssetIndexResult implements Iterable<Asset> {
    @NotNull
    private final AssetSelectorExpression expression;
    @NotNull
    private final Iterable<Asset> assets;
    @Nullable
    private final Cursor nextCursor;

    public AssetIndexResult(
            @NotNull AssetSelectorExpression expression,
            @NotNull Iterable<Asset> assets,
            @Nullable Cursor nextCursor) {
        this.expression = Objects.requireNonNull(expression);
        this.assets = Objects.requireNonNull(assets);
        this.nextCursor = nextCursor;
    }

    public @NotNull AssetSelectorExpression getExpression() {
        return expression;
    }

    public @Nullable Cursor getNextCursor() {
        return nextCursor;
    }

    @NotNull
    @Override
    public Iterator<Asset> iterator() {
        return assets.iterator();
    }

    @JsonPOJOBuilder
    public static class Builder {
        private Iterable<Asset> assets;
        private Cursor nextCursor;
        private AssetSelectorExpression expression;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            assets = new ArrayList<>();
        }

        public Builder assets(Iterable<Asset> assets) {
            this.assets = assets;
            return this;
        }

        public Builder nextCursor(final Cursor nextCursor) {
            this.nextCursor = nextCursor;
            return this;
        }


        public Builder expression(AssetSelectorExpression expression) {
            this.expression = expression;
            return this;
        }

        public AssetIndexResult build() {
            return new AssetIndexResult(
                    expression,
                    assets,
                    nextCursor);
        }
    }
}
