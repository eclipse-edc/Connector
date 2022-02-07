package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.CriterionConverter;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Arrays;
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
            return asset -> {
                Object property = property((String) criterion.getOperandLeft(), asset);
                if (property == null) {
                    return false; //property does not exist on asset
                }
                return Objects.equals(property, criterion.getOperandRight());
            };
        } else if ("in".equalsIgnoreCase(criterion.getOperator())) {
            return asset -> {
                String property = property((String) criterion.getOperandLeft(), asset);
                var list = (String) criterion.getOperandRight();
                // some cleanup needs to happen
                list = list.replace("(", "").replace(")", "").replace(" ", "");
                var items = list.split(",");
                return Arrays.asList(items).contains(property);
            };
        }
        throw new IllegalArgumentException(String.format("Operator [%s] is not supported by this converter!", criterion.getOperator()));
    }


    private <T> T property(String key, Asset asset) {
        if (asset.getProperties() == null || asset.getProperties().isEmpty()) {
            return null;
        }
        return (T) asset.getProperty(key);
    }
}
