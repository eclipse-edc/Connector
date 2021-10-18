package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.asset.CriterionConverter;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Converts a {@link Criterion}, which is essentially a select statement, into a {@code Predicate<Asset>}.
 * <p>
 * This is useful when dealing with in-memory collections of objects, here: {@link Asset} where Predicates can be applied
 * efficiently.
 * <p>
 * _Note: other {@link org.eclipse.dataspaceconnector.spi.asset.AssetIndex} implementations might have different converters!
 */
public class CriterionToPredicateConverter implements CriterionConverter<Predicate<Asset>> {
    @Override
    public Predicate<Asset> convert(Criterion criterion) {
        if ("=".equals(criterion.getOperator())) {
            return asset -> Objects.equals(field((String) criterion.getOperandLeft(), asset), criterion.getOperandRight()) ||
                    Objects.equals(label((String) criterion.getOperandLeft(), asset), criterion.getOperandRight());
        }
        throw new IllegalArgumentException(String.format("Operator [%s] is not supported by this converter!", criterion.getOperator()));
    }

    private Object field(String fieldName, Asset asset) {
        try {
            Field declaredField = asset.getClass().getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            return declaredField.get(asset);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return null;
        }
    }

    private Object label(String key, Asset asset) {
        if (asset.getProperties() == null || !asset.getProperties().isEmpty()) {
            return null;
        }
        return asset.getProperty(key);
    }
}
