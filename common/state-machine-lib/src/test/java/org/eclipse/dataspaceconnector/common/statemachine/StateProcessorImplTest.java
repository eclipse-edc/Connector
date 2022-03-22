package org.eclipse.dataspaceconnector.common.statemachine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StateProcessorImplTest {

    @Test
    void shouldReturnTheProcessedCount() {
        var processor = new StateProcessorImpl<>(() -> List.of("any"), string -> true);

        var count = processor.process();

        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldNotCountUnprocessedEntities() {
        var processor = new StateProcessorImpl<>(() -> List.of("any"), string -> false);

        var count = processor.process();

        assertThat(count).isEqualTo(0);
    }
}
