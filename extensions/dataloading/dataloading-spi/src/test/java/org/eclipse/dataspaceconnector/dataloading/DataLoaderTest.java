package org.eclipse.dataspaceconnector.dataloading;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;

class DataLoaderTest {

    private static final String INDEX_VALIDATION_MESSAGE = "index must be > 0!";
    private static final String DESCRIPTION_VALIDATION_MESSAGE = "Description cannot be null!";
    private DataLoader<TestEntity> dataLoader;
    private TestEntitySink sinkMock;

    @BeforeEach
    void setUp() {
        sinkMock = strictMock(TestEntitySink.class);
        DataLoader.Builder<TestEntity> builder = DataLoader.Builder.newInstance();
        dataLoader = builder.sink(sinkMock)
                .andPredicate(testEntity -> testEntity.getDescription() != null ? ValidationResult.OK : ValidationResult.error(DESCRIPTION_VALIDATION_MESSAGE))
                .andPredicate(testEntity -> testEntity.getIndex() > 0 ? ValidationResult.OK : ValidationResult.error(INDEX_VALIDATION_MESSAGE))
                .build();
    }

    @Test
    void insert() {
        var te = new TestEntity("Test Desc", 3);
        sinkMock.accept(te);
        expectLastCall();
        replay(sinkMock);
        dataLoader.insert(te);
        verify(sinkMock);
    }

    @Test
    void insert_oneValidationFails() {
        var te = new TestEntity("Test Desc", -3);
        replay(sinkMock);

        assertThatThrownBy(() -> dataLoader.insert(te)).isInstanceOf(ValidationException.class)
                .hasMessage(INDEX_VALIDATION_MESSAGE);
        verify(sinkMock);
    }

    @Test
    void insert_multipleValidationsFail() {
        var te = new TestEntity(null, -3);
        replay(sinkMock);

        assertThatThrownBy(() -> dataLoader.insert(te)).isInstanceOf(ValidationException.class)
                .hasMessageContaining(INDEX_VALIDATION_MESSAGE)
                .hasMessageContaining(DESCRIPTION_VALIDATION_MESSAGE);
        verify(sinkMock);
    }

    @Test
    void insertAll() {
        var items = IntStream.range(1, 10).mapToObj(i -> new TestEntity("Test Item " + i, i)).collect(Collectors.toList());
        sinkMock.accept(anyObject(TestEntity.class));
        expectLastCall().times(9);
        replay(sinkMock);
        dataLoader.insertAll(items);
        verify(sinkMock);
    }

    @Test
    void insertAll_oneItemFails() {
        var items = IntStream.range(1, 10).mapToObj(i -> new TestEntity("Test Item " + i, i)).collect(Collectors.toList());
        items.add(new TestEntity("Invalid entity", -9));
        replay(sinkMock);
        assertThatThrownBy(() -> dataLoader.insertAll(items)).isInstanceOf(ValidationException.class)
                .hasMessageContaining(INDEX_VALIDATION_MESSAGE);
        verify(sinkMock);
    }

    @Test
    void insertAll_oneItemFailsWithMultipleValidationErrors() {
        var items = IntStream.range(1, 10).mapToObj(i -> new TestEntity("Test Item " + i, i)).collect(Collectors.toList());
        items.add(new TestEntity("Invalid entity", -9));
        items.add(new TestEntity(null, -9));

        replay(sinkMock);
        assertThatThrownBy(() -> dataLoader.insertAll(items)).isInstanceOf(ValidationException.class)
                .hasMessageContaining(INDEX_VALIDATION_MESSAGE)
                .hasMessageContaining(DESCRIPTION_VALIDATION_MESSAGE);
        verify(sinkMock);
    }

    @Test
    void insertAll_multipleItemsFail() {
        var items = IntStream.range(1, 10).mapToObj(i -> new TestEntity(null, i)).collect(Collectors.toList());
        replay(sinkMock);
        assertThatThrownBy(() -> dataLoader.insertAll(items)).isInstanceOf(ValidationException.class)
                .hasMessageContaining(DESCRIPTION_VALIDATION_MESSAGE);
        verify(sinkMock);
    }
}