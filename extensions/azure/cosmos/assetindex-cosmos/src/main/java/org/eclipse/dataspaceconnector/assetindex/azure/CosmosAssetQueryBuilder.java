package org.eclipse.dataspaceconnector.assetindex.azure;

import com.azure.cosmos.models.SqlQuerySpec;
import org.eclipse.dataspaceconnector.assetindex.azure.model.AssetDocument;
import org.eclipse.dataspaceconnector.azure.cosmos.dialect.SqlStatement;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CosmosAssetQueryBuilder {

    public CosmosAssetQueryBuilder() {
        // pre-check fields of the AssetDocument class that will be used to build the queries
        assertClassContainsField(AssetDocument.class, "wrappedInstance");
    }

    private static void assertClassContainsField(Class<?> clazz, String fieldName) {
        if (getDeclaredFields(clazz).stream().noneMatch(field -> field.getName().equals(fieldName))) {
            throw new EdcException("Cannot find field " + fieldName + " in class " + clazz.getSimpleName());
        }
    }

    @NotNull
    private static List<Field> getDeclaredFields(Class<?> clazz) {
        var fields = new ArrayList<Field>();
        return getAllFields(fields, clazz);
    }

    public static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    public SqlQuerySpec from(List<Criterion> criteria) {
        return new SqlStatement<>(AssetDocument.class)
                .where(criteria)
                .getQueryAsSqlQuerySpec();
    }

    public SqlQuerySpec from(List<Criterion> criteria, String orderByField, boolean sortAscending, Integer limit, Integer offset) {

        var stmt = new SqlStatement<>(AssetDocument.class)
                .where(criteria)
                .orderBy(orderByField, sortAscending)
                .offset(offset)
                .limit(limit);

        return stmt.getQueryAsSqlQuerySpec();
    }

}
