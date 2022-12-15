package org.eclipse.edc.transform.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransformerContextImplTest {

    private final TypeTransformerRegistry<?> registry = mock(TypeTransformerRegistry.class);
    private final TransformerContextImpl context = new TransformerContextImpl(registry);

    @Test
    void shouldReturnTransformedInput() {
        when(registry.transformerFor(anyString(), eq(Integer.class))).thenReturn(new StringIntegerTypeTransformer());

        var result = context.transform("5", Integer.class);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void shouldCollectProblems_whenTransformFails() {
        when(registry.transformerFor(anyString(), eq(Integer.class))).thenReturn(new StringIntegerTypeTransformer());

        var result = context.transform("not an integer", Integer.class);

        assertThat(result).isEqualTo(null);
        assertThat(context.getProblems()).hasSize(1);
    }

    @Test
    void shouldNotTransform_whenInputIsNull() {
        var result = context.transform(null, Integer.class);

        assertThat(result).isNull();
        verifyNoInteractions(registry);
    }
}
