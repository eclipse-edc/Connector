package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.catalog.spi.model.RecurringExecutionPlan;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.time.Duration;
import java.util.Random;

/**
 * Object that provides configuration for the {@link PartitionManager}.
 * All configuration values that do not allow for default values are resolved instantly, all others are resolved
 * lazily from the context.
 */
public class PartitionConfiguration {

    private static final String PART_WORK_ITEM_QUEUE_SIZE_SETTING = "edc.catalog.cache.partition.queue-size";
    private static final String PART_NUM_CRAWLER_SETTING = "edc.catalog.cache.partition.num-crawlers";
    private static final String PART_LOADER_BATCH_SIZE_SETTING = "edc.catalog.cache.loader.batch-size";
    private static final String PART_LOADER_RETRY_TIMEOUT = "edc.catalog.cache.loader.timeout-millis";
    private static final String PART_EXECUTION_PLAN_PERIOD_MINUTES = "edc.catalog.cache.execution.period-minutes";
    private static final String PART_EXECUTION_PLAN_DELAY_MINUTES = "edc.catalog.cache.execution.delay-minutes";
    private final ServiceExtensionContext context;

    public PartitionConfiguration(ServiceExtensionContext context) {
        this.context = context;
    }

    public int getWorkItemQueueSize(int defaultValue) {
        return Integer.parseInt(context.getSetting(PART_WORK_ITEM_QUEUE_SIZE_SETTING, String.valueOf(defaultValue)));
    }


    public int getNumCrawlers(int defaultValue) {
        return Integer.parseInt(context.getSetting(PART_NUM_CRAWLER_SETTING, String.valueOf(defaultValue)));
    }


    public int getLoaderBatchSize(int defaultValue) {
        return Integer.parseInt(context.getSetting(PART_LOADER_BATCH_SIZE_SETTING, String.valueOf(defaultValue)));
    }

    public long getLoaderRetryTimeout(int defaultValue) {
        return Integer.parseInt(context.getSetting(PART_LOADER_RETRY_TIMEOUT, String.valueOf(defaultValue)));
    }

    public ExecutionPlan getExecutionPlan() {
        var minutes = Integer.parseInt(context.getSetting(PART_EXECUTION_PLAN_PERIOD_MINUTES, "10"));
        var setting = context.getSetting(PART_EXECUTION_PLAN_DELAY_MINUTES, null);
        int initialDelaySeconds;
        if ("random".equals(setting) || setting == null) {
            initialDelaySeconds = randomSeconds();
        } else {
            try {
                initialDelaySeconds = Integer.parseInt(setting);
            } catch (NumberFormatException ex) {
                initialDelaySeconds = 0;
            }
        }
        return new RecurringExecutionPlan(Duration.ofMinutes(minutes), Duration.ofSeconds(initialDelaySeconds));
    }

    private int randomSeconds() {
        var rnd = new Random();
        return 10 + rnd.nextInt(90);
    }
}
