package org.eclipse.dataspaceconnector.assetindex.azure;

import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import org.eclipse.dataspaceconnector.assetindex.azure.model.AssetDocument;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CosmosAssetQueryBuilder {

    private static final String PROPERTIES_FIELD = "wrappedInstance";
    private static final String PATH_TO_PROPERTIES = String.join(".", AssetDocument.class.getSimpleName(), PROPERTIES_FIELD);

    public CosmosAssetQueryBuilder() {
        // pre-check fields of the AssetDocument class that will be used to build the queries
        assertClassContainsField(AssetDocument.class, PROPERTIES_FIELD);
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

    public SqlQuerySpec from(AssetSelectorExpression expression) {
        WhereClause whereClause = new WhereClause(expression);
        return new SqlQuerySpec("SELECT * FROM " + AssetDocument.class.getSimpleName() + whereClause.getWhere(), whereClause.getParameters());
    }

    private static class WhereClause {
        private static final List<String> SUPPORTED_OPERATOR = Collections.singletonList("=");
        private final List<SqlParameter> parameters = new ArrayList<>();
        private String where = "";

        public WhereClause(AssetSelectorExpression expression) {
            if (expression != AssetSelectorExpression.SELECT_ALL) {
                expression.getCriteria().forEach(this::criterion);
            }
        }

        private static String formatValue(String value) {
            return "'" + value + "'";
        }

        public String getWhere() {
            return where;
        }

        public List<SqlParameter> getParameters() {
            return parameters;
        }

        private void criterion(Criterion criterion) {
            if (!SUPPORTED_OPERATOR.contains(criterion.getOperator())) {
                throw new EdcException("Cannot build SqlParameter for operator: " + criterion.getOperator());
            }
            String field = AssetDocument.sanitize(criterion.getOperandLeft().toString());
            String param = "@" + field;
            where += parameters.isEmpty() ? " WHERE" : " AND";
            parameters.add(new SqlParameter(param, criterion.getOperandRight().toString()));
            where += String.join(" " + criterion.getOperator() + " ",
                    " " + PATH_TO_PROPERTIES + "." + field,
                    formatValue((String) criterion.getOperandRight()));
        }
    }
}
