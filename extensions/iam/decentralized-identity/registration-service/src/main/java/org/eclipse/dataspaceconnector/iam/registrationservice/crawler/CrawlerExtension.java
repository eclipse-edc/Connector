/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.registrationservice.crawler;

import org.eclipse.dataspaceconnector.events.azure.AzureEventGridConfig;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.iam.did.spi.store.DidStore;
import org.eclipse.dataspaceconnector.iam.registrationservice.events.CrawlerEventPublisher;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import java.time.Clock;
import java.util.Map;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class CrawlerExtension implements ServiceExtension {

    @EdcSetting
    private static final String CRAWLER_INTERVAL_MIN_SETTING = "edc.ion.crawler.interval-minutes";
    @EdcSetting
    private static final String ION_URL_SETTING = "edc.ion.crawler.ion.url";
    @EdcSetting
    private static final String ION_CRAWLER_TYPE_SETTING = "edc.ion.crawler.did-type";
    private ServiceExtensionContext context;
    private Scheduler quartzScheduler;
    @Inject
    private DidStore didStore;
    @Inject
    private DidResolverRegistry resolverRegistry;
    @Inject
    private Vault vault;
    @Inject
    private Clock clock;

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.context = context;

        context.getMonitor().info("ION Crawler Extension initialized");
    }

    @Override
    public void start() {
        try {
            quartzScheduler = StdSchedulerFactory.getDefaultScheduler();
            quartzScheduler.start();

            var minutes = context.getSetting(CRAWLER_INTERVAL_MIN_SETTING, 30);

            scheduleCrawler(minutes, context);
            context.getMonitor().info("ION Crawler Extension started");
            context.getMonitor().info("Started periodic crawling of the ION database every " + minutes + " minutes");

        } catch (SchedulerException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void shutdown() {
        try {
            context.getMonitor().info("Stopping ION crawler");
            quartzScheduler.shutdown();
            context.getMonitor().info("Stopped ION crawler");
            context.getMonitor().info("ION Crawler Extension shutdown");
        } catch (SchedulerException e) {
            throw new EdcException(e);
        }
    }

    private void scheduleCrawler(int intervalMinutes, ServiceExtensionContext context) throws SchedulerException {

        var publisher = new CrawlerEventPublisher(vault, new AzureEventGridConfig(context));

        var crawlerConfig = CrawlerContext.Builder.create()
                .didStore(didStore)
                .ionHost(context.getSetting(ION_URL_SETTING, "http://gx-ion-node.westeurope.cloudapp.azure.com:3000/"))
                .monitor(context.getMonitor())
                .typeManager(context.getTypeManager())
                .clock(clock)
                .publisher(publisher)
                .didTypes(context.getSetting(ION_CRAWLER_TYPE_SETTING, "aW9u"))
                .resolverRegistry(resolverRegistry)
                .build();

        JobDetail job = newJob(CrawlerJob.class)
                .setJobData(new JobDataMap(Map.of(CrawlerContext.KEY, crawlerConfig)))
                .withIdentity("ion-crawler-job", "ion")
                .build();
        Trigger trigger = newTrigger()
                .withIdentity("ion-crawler-trigger", "ion")
                .startNow()
                .withSchedule(simpleSchedule().withIntervalInMinutes(intervalMinutes).repeatForever())
                .build();

        quartzScheduler.scheduleJob(job, trigger);
    }


}
