package org.eclipse.dataspaceconnector.spi.asset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.pagination.Cursor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Query for the {@link AssetIndex}. Limit must be defined, otherwise its 0.
 */
public class AssetIndexQuery {
    @NotNull
    private final AssetSelectorExpression expression;
    private final long limit;
    @Nullable
    private final Cursor nextCursor;


    private AssetIndexQuery(@NotNull AssetSelectorExpression expression,
                            long limit,
                            @Nullable Cursor cursor) {
        this.expression = Objects.requireNonNull(expression);
        this.limit = limit;
        this.nextCursor = cursor;
    }

    public @NotNull AssetSelectorExpression getExpression() {
        return expression;
    }

    public long getLimit() {
        return limit;
    }

    public @Nullable Cursor getNextCursor() {
        return nextCursor;
    }

    @JsonPOJOBuilder
    public static class Builder {
        private AssetSelectorExpression expression;
        private long limit;
        private Cursor nextCursor;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        public Builder expression(AssetSelectorExpression expression) {
            this.expression = expression;
            return this;
        }

        public Builder limit(long limit) {
            this.limit = limit;
            return this;
        }

        public Builder nextCursor(Cursor nextCursor) {
            this.nextCursor = nextCursor;
            return this;
        }

        public AssetIndexQuery build() {
            return new AssetIndexQuery(
                    expression,
                    limit,
                    nextCursor);
        }

    }
}
