package org.eclipse.dataspaceconnector.common.statemachine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntitiesProcessorImplTest {

    @Test
    void shouldReturnTheProcessedCount() {
        var processor = new EntitiesProcessorImpl<>(() -> List.of("any"), string -> true);

        var count = processor.run();

        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldNotCountUnprocessedEntities() {
        var processor = new EntitiesProcessorImpl<>(() -> List.of("any"), string -> false);

        var count = processor.run();

        assertThat(count).isEqualTo(0);
    }
}
