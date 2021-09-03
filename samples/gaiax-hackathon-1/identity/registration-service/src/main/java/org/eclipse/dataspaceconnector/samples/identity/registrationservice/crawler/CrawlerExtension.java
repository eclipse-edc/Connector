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

package org.eclipse.dataspaceconnector.samples.identity.registrationservice.crawler;

import org.eclipse.dataspaceconnector.events.azure.AzureEventGridConfig;
import org.eclipse.dataspaceconnector.iam.ion.IonClientImpl;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.DidDocument;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.Service;
import org.eclipse.dataspaceconnector.iam.ion.spi.DidStore;
import org.eclipse.dataspaceconnector.samples.identity.registrationservice.events.CrawlerEventPublisher;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class CrawlerExtension implements ServiceExtension {
    @EdcSetting
    private static final String LOAD_SAMPLE_DATA = "edc.ion.crawler.sample.data";

    @EdcSetting
    private static final String CRAWLER_INTERVAL_MIN_SETTING = "edc.ion.crawler.interval-minutes";
    @EdcSetting
    private static final String ION_URL_SETTING = "edc.ion.crawler.ion.url";
    @EdcSetting
    private static final String ION_RANDOMIZE_DID_DOCUMENTS_SETTING = "edc.ion.crawler.randomize";
    @EdcSetting
    private static final String ION_GAIAX_TYPE_SETTING = "Z3hp";
    private ServiceExtensionContext context;
    private Scheduler quartzScheduler;

    @Override
    public Set<String> provides() {
        return Set.of("edc:registration-service");
    }

    @Override
    public Set<String> requires() {
        return Set.of("edc:did-documentstore");
    }


    @Override
    public void initialize(ServiceExtensionContext context) {
        this.context = context;

        // create the crawler that periodically browses ION for new DIDs
        var ionClient = new IonClientImpl(context.getTypeManager().getMapper());

        context.getMonitor().info("ION Crawler Extension initialized");
    }

    @Override
    public void start() {
        DidStore didStore = context.getService(DidStore.class);
        if (Boolean.parseBoolean(context.getSetting(LOAD_SAMPLE_DATA, "false"))) {
            registerSampleDids(didStore);
        }
        try {
            quartzScheduler = StdSchedulerFactory.getDefaultScheduler();
            quartzScheduler.start();

            var minutes = Integer.parseInt(context.getSetting(CRAWLER_INTERVAL_MIN_SETTING, "30"));

            scheduleCrawler(minutes, didStore, context);
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

    private void scheduleCrawler(int intervalMinutes, DidStore objectStore, ServiceExtensionContext context) throws SchedulerException {

        var publisher = new CrawlerEventPublisher(context.getService(Vault.class), new AzureEventGridConfig(context));

        var crawlerConfig = CrawlerContext.Builder.create()
                .didStore(objectStore)
                .ionHost(context.getSetting(ION_URL_SETTING, "http://gx-ion-node.westeurope.cloudapp.azure.com:3000/"))
                .monitor(context.getMonitor())
                .publisher(publisher)
                .randomize(Boolean.parseBoolean(context.getSetting(ION_RANDOMIZE_DID_DOCUMENTS_SETTING, "false")))
                .didTypes(context.getSetting(ION_GAIAX_TYPE_SETTING, "Z3hp")) //Z3hp is base64 for "gxi", which is GaiaX-Identity type
                .ionClient(new IonClientImpl(context.getTypeManager().getMapper())) //can be null if randomize = true
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

    // TODO HACKATHON-1 TASK 1 remove when DIDs registered
    private void registerSampleDids(DidStore didStore) {
        context.getMonitor().info("Registering consumer test DID");

        // Resolve ION/IdentityHub discrepancy
        var hubService = new Service("IdentityHub", "IdentityHub", "http://localhost:9191/api/identity-hub");

        // Resolve ION/IdentityHub discrepancy
        var catalogService = new Service("GaiaXCatalog", "GaiaXCatalog", "http://localhost:9191/api/catalog");

        var didDocument = DidDocument.Builder.newInstance().id("did:ion:123consumer").service(List.of(hubService, catalogService)).build();

        didStore.saveAll(List.of(didDocument));
    }

}
