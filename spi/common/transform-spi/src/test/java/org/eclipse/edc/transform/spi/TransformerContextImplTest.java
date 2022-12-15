package org.eclipse.edc.transform.spi;

import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransformerContextImplTest {

    private final TypeTransformerRegistry<?> registry = mock(TypeTransformerRegistry.class);
    private final TransformerContextImpl context = new TransformerContextImpl(registry);

    @Test
    void shouldReturnTransformedInput() {
        when(registry.transform("5", Integer.class)).thenReturn(Result.success(5));

        var result = context.transform("5", Integer.class);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void shouldCollectProblems_whenTransformFails() {
        when(registry.transform(any(), any())).thenReturn(Result.failure("a problem"));

        var result = context.transform("5", Integer.class);

        assertThat(result).isEqualTo(null);
        assertThat(context.getProblems()).containsExactly("a problem");
    }

    @Test
    void shouldNotTransform_whenInputIsNull() {
        var result = context.transform(null, Integer.class);

        assertThat(result).isNull();
        verifyNoInteractions(registry);
    }
}
