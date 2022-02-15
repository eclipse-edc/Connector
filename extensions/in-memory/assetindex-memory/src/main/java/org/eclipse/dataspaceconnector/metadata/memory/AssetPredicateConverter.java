package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.query.BaseCriterionToPredicateConverter;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

/**
 * Converts a {@link Criterion}, which is essentially a select statement, into a {@code Predicate<Asset>}.
 * <p>
 * This is useful when dealing with in-memory collections of objects, here: {@link Asset} where Predicates can be applied
 * efficiently.
 * <p>
 * _Note: other {@link org.eclipse.dataspaceconnector.spi.asset.AssetIndex} implementations might have different converters!
 */
public class AssetPredicateConverter extends BaseCriterionToPredicateConverter<Asset> {
    @Override
    public <T> T property(String key, Object object) {
        if (object instanceof Asset) {
            var asset = (Asset) object;
            if (asset.getProperties() == null || asset.getProperties().isEmpty()) {
                return null;
            }
            return (T) asset.getProperty(key);
        }
        throw new IllegalArgumentException("Can only handle objects of type " + Asset.class.getSimpleName() + " but received an " + object.getClass().getSimpleName());
    }
}
