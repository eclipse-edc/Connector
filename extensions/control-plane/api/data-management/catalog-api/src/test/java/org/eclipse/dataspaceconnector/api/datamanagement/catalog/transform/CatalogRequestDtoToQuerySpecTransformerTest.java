package org.eclipse.dataspaceconnector.api.datamanagement.catalog.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.catalog.model.CatalogRequestDto;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.api.datamanagement.catalog.TestFunctions.createCriterionDto;
import static org.mockito.Mockito.mock;

class CatalogRequestDtoToQuerySpecTransformerTest {

    private CatalogRequestDtoToQuerySpecTransformer transformer;

    @BeforeEach
    void setup() {
        transformer = new CatalogRequestDtoToQuerySpecTransformer();
    }

    @Test
    void verifyInputType() {
        assertThat(transformer.getInputType()).isEqualTo(CatalogRequestDto.class);
    }

    @Test
    void verifyOutputType() {
        assertThat(transformer.getOutputType()).isEqualTo(QuerySpec.class);
    }

    @Test
    void transform() {
        var dto = CatalogRequestDto.Builder.newInstance()
                .providerUrl("test-providerUrl")
                .sortOrder(SortOrder.ASC)
                .sortField("test-sortfield")
                .limit(5)
                .offset(1)
                .filter(List.of(createCriterionDto("foo", "=", "bar"), createCriterionDto("bar", "in", Arrays.asList("baz", "boz", "buz"))))
                .build();

        var ctx = mock(TransformerContext.class);
        var tf = transformer.transform(dto, ctx);

        assertThat(tf).isNotNull();
        assertThat(tf.getLimit()).isEqualTo(dto.getLimit());
        assertThat(tf.getOffset()).isEqualTo(dto.getOffset());
        assertThat(tf.getFilterExpression()).hasSize(2);
        assertThat(tf.getSortField()).isEqualTo(dto.getSortField());
        assertThat(tf.getSortOrder()).isEqualTo(dto.getSortOrder());
    }

    @Test
    void transform_withoutFilter() {
        var dto = CatalogRequestDto.Builder.newInstance()
                .providerUrl("test-providerUrl")
                .sortOrder(SortOrder.ASC)
                .sortField("test-sortfield")
                .limit(5)
                .offset(1)
                .build();

        var ctx = mock(TransformerContext.class);
        var tf = transformer.transform(dto, ctx);

        assertThat(tf).isNotNull();
        assertThat(tf.getLimit()).isEqualTo(dto.getLimit());
        assertThat(tf.getOffset()).isEqualTo(dto.getOffset());
        assertThat(tf.getFilterExpression()).isNotNull().isEmpty();
        assertThat(tf.getSortField()).isEqualTo(dto.getSortField());
        assertThat(tf.getSortOrder()).isEqualTo(dto.getSortOrder());
    }
}