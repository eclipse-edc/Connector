package org.eclipse.dataspaceconnector.core.manager;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntitiesProcessorTest {

    @Test
    void shouldReturnTheProcessedCount() {
        var processor = new EntitiesProcessor<>(() -> List.of("any"));

        var count = processor.doProcess(string -> true);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldNotCountUnprocessedEntities() {
        var processor = new EntitiesProcessor<>(() -> List.of("any"));

        var count = processor.doProcess(string -> false);

        assertThat(count).isEqualTo(0);
    }
}