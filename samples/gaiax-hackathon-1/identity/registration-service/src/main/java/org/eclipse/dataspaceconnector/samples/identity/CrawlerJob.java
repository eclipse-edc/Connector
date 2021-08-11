package org.eclipse.dataspaceconnector.samples.identity;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

// has to be "public", otherwise quartz won't be able to access is
public class CrawlerJob implements Job {
    @Override
    public void execute(JobExecutionContext context) {
        var store = (DidStore) context.getJobDetail().getJobDataMap().get("STORE");
        var monitor = (Monitor) context.getJobDetail().getJobDataMap().get("MONITOR");

        monitor.info("Crawling ION to obtain new DIDs");
    }
}
