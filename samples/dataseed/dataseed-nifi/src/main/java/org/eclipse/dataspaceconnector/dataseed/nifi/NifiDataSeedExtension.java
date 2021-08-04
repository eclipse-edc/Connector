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

package org.eclipse.dataspaceconnector.dataseed.nifi;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataseed.nifi.api.NifiApiClient;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;

public class NifiDataSeedExtension implements ServiceExtension {

    private static final String DEFAULT_NIFI_URL = "http://localhost:8080";
    private static final String NIFI_URL_PROPERTY = "edc.nifi.url";
    private final static String rootProcessGroup = "root";
    private NifiApiClient nifiApiClient;
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {

        monitor = context.getMonitor();
        try {
            nifiApiClient = new NifiApiClient(context.getSetting(NIFI_URL_PROPERTY, DEFAULT_NIFI_URL), context.getTypeManager(), context.getService(OkHttpClient.class));
        } catch (MalformedURLException e) {
            monitor.severe("NifiDataSeedExtension: Exception during seeting Nifi data: ", e);
        }
    }

    @Override
    public Set<String> requires() {
        return Set.of("dataspaceconnector:http-client");
    }

    @Override
    public void start() {
        monitor.info("Starting Nifi data seeding");
        if (nifiApiClient != null) {
            String processGroup = getProcessGroup();

            try {
                var templateid = nifiApiClient.uploadTemplate(processGroup, getTemplateFile());
                nifiApiClient.instantiateTemplate(templateid);
                var pg = nifiApiClient.getProcessGroup(processGroup);

                monitor.info("NifiDataSeed: start controller service");
            } catch (EdcException ex) {
//                monitor.severe("NifiDataSeed: updload template error: ", ex);
            } finally {
                var controllerService = nifiApiClient.getControllerServices(processGroup).get(0);
                var version = controllerService.revision.version;
                var controllerServiceId = controllerService.id;
                nifiApiClient.startControllerService(controllerServiceId, version);
                monitor.info("NifiDataSeed: start process group");
                nifiApiClient.startProcessGroup(processGroup);
            }
        } else {
            monitor.severe("NifiDataSeed: cannot seed data to nifi - error instantiating the api client!");
        }
    }

    private File getTemplateFile() {
        var fileStream = getClass().getClassLoader().getResourceAsStream("ThreeClouds.xml");
        try {
            var file = File.createTempFile("temp", Long.toString(System.nanoTime()));

            Files.copy(fileStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return file;
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    private String getProcessGroup() {
        return rootProcessGroup;
    }
}
