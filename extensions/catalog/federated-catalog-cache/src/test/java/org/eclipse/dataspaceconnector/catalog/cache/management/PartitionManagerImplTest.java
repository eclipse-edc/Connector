package org.eclipse.dataspaceconnector.catalog.cache.management;

import org.easymock.EasyMock;
import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.easymock.MockType;
import org.eclipse.dataspaceconnector.catalog.cache.TestWorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;

/**
 * Unit test for the partition manager
 */
@ExtendWith(EasyMockExtension.class)
public class PartitionManagerImplTest {

    private PartitionManagerImpl partitionManager;
    @Mock(type = MockType.NICE)
    private Monitor monitorMock;
    @Mock(type = MockType.STRICT)
    private WorkItemQueue workItemQueueMock;
    private List<WorkItem> staticWorkload;

    @BeforeEach
    void setup() {
        staticWorkload = List.of(new TestWorkItem());
        partitionManager = new PartitionManagerImpl(monitorMock, workItemQueueMock, mockCrawler(), 5, staticWorkload);
    }

    @NotNull
    private Function<WorkItemQueue, Crawler> mockCrawler() {
        return workItems -> {
            Crawler mock = niceMock(Crawler.class);
            replay(mock);
            return mock;
        };
    }

    @Test
    @DisplayName("expect the workload to be put into the work item queue")
    void schedule() {
        workItemQueueMock.lock();
        expectLastCall();
        expect(workItemQueueMock.addAll(staticWorkload)).andReturn(true);
        workItemQueueMock.unlock();
        expectLastCall();
        replay(workItemQueueMock);

        partitionManager.schedule(Runnable::run);

        verify(workItemQueueMock);
    }

    @Test
    void update() {
        ExecutionPlan newPlan = strictMock(ExecutionPlan.class);
        replay(newPlan);

        assertThat(partitionManager.update(newPlan)).isEqualTo(newPlan);
        verify(newPlan);
    }

    @Test
    void update_multipleTimes() {
        ExecutionPlan plan1 = strictMock(ExecutionPlan.class);
        ExecutionPlan plan2 = strictMock(ExecutionPlan.class);

        expect(plan1.merge(plan2)).andReturn(plan2);
        replay(plan1, plan2);

        assertThat(partitionManager.update(plan1)).isEqualTo(plan1);
        assertThat(partitionManager.update(plan2)).isEqualTo(plan2);

        verify(plan1, plan2);

    }

    @Test
    void stop_allCrawlersJoinSuccessfully(){
        List<Crawler> list = new ArrayList<>();
        partitionManager = new PartitionManagerImpl(monitorMock, workItemQueueMock, workItems -> {
            Crawler crawler = strictMock(Crawler.class);
            crawler.run();
            expectLastCall();
            expect(crawler.join()).andReturn(true);
            replay(crawler);
            list.add(crawler);
            return crawler;
        }, 5, staticWorkload);

        partitionManager.stop();
        list.forEach(EasyMock::verify);
    }
}
