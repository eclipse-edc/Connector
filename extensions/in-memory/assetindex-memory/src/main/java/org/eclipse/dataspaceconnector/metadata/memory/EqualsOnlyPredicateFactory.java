package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Predicate;

public class EqualsOnlyPredicateFactory implements PredicateFactory<Asset> {
    @Override
    public Predicate<Asset> convert(Criterion criterion) {
        if ("=".equals(criterion.getOperator())) {
            return asset -> Objects.equals(field(criterion.getOperandLeft(), asset), criterion.getOperandRight()) ||
                    Objects.equals(label(criterion.getOperandLeft(), asset), criterion.getOperandRight());
        }
        return (asset) -> false;

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

    private String label(String key, Asset asset) {
        if (asset.getLabels() == null || !asset.getLabels().isEmpty()) {
            return null;
        }
        return asset.getLabels().get(key);
    }
}
