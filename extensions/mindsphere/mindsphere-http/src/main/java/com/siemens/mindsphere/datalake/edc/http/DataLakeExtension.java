/*
 *  Copyright (c) 2021, 2022 Siemens AG
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

package com.siemens.mindsphere.datalake.edc.http;

import com.siemens.mindsphere.datalake.edc.http.provision.DestinationUrlProvisioner;
import com.siemens.mindsphere.datalake.edc.http.provision.DestinationUrlResourceDefinitionGenerator;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class DataLakeExtension implements ServiceExtension {
    @EdcSetting
    private static final String STUB_URL = "edc.demo.http.destination.url";

    @EdcSetting
    private static final String TOKENMANAGEMENT_ADDRESS = "datalake.oauth.address";
    
    @EdcSetting
    private static final String TOKENMANAGEMENT_CLIENT_ID = "datalake.oauth.client.id";
    
    @EdcSetting
    private static final String TOKENMANAGEMENT_CLIENT_SECRET = "datalake.oauth.client.secret";

    @EdcSetting
    private static final String TOKENMANAGEMENT_CLIENT_APP_NAME = "datalake.oauth.client.app.name";

    @EdcSetting
    private static final String TOKENMANAGEMENT_CLIENT_APP_VERSION = "datalake.oauth.client.app.version";

    @EdcSetting
    private static final String APPLICATION_TENANT = "datalake.tenant";

    @EdcSetting
    private static final String SNS_TOPIC_ARN = "datalake.event.destination";

    @EdcSetting
    private static final String DATALAKE_ADDRESS = "datalake.address";

    @Inject
    private DataOperatorRegistry dataOperatorRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        Monitor monitor = context.getMonitor();
        // create Data Lake client
        final String destinationUrl = context.getSetting(STUB_URL, "http://missing");


        final String tokenmanagementClientId = context.getSetting(TOKENMANAGEMENT_CLIENT_ID, "");
        final String tokenmanagementClientSecret = context.getSetting(TOKENMANAGEMENT_CLIENT_SECRET, "");
        final String tokenmanagementClientAppName = context.getSetting(TOKENMANAGEMENT_CLIENT_APP_NAME, "");
        final String tokenmanagementClientAppVersion = context.getSetting(TOKENMANAGEMENT_CLIENT_APP_VERSION, "");
        final String tokenmanagementAddress = context.getSetting(TOKENMANAGEMENT_ADDRESS, "");
        final String dataLakeAddress = context.getSetting(DATALAKE_ADDRESS, "");

        final String applicationTenant = context.getSetting(APPLICATION_TENANT, "presdev");
        final String snsTopicArn = context.getSetting(SNS_TOPIC_ARN, "");
       
        System.out.println("datalake.oauth.clientId=" + tokenmanagementClientId);
        System.out.println("datalake.oauth.clientSecret=" + tokenmanagementClientSecret);
        System.out.println("datalake.tenant=" + applicationTenant);
        System.out.println("datalake.address=" + dataLakeAddress);

        final URL url;
        try {
            url = new URL(dataLakeAddress);
            final OauthClientDetails oauthClientDetails = new OauthClientDetails(tokenmanagementClientId, tokenmanagementClientSecret,
            tokenmanagementClientAppName, tokenmanagementClientAppVersion, applicationTenant, new URL(tokenmanagementAddress));
            DataLakeClientImpl clientImpl = new DataLakeClientImpl(oauthClientDetails, new URL(dataLakeAddress));
            DataLakeClientImpl.setInstance(clientImpl);
            URL createdUrl = clientImpl.getUrl("{\"paths\":[{\"path\":\"data/ten=castidev/data.csv\"}]}");
            System.out.println("Created presigned url: " + createdUrl.toString());
        } catch (MalformedURLException  | DataLakeException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Bad destination url given", e);
        }

        final DataLakeClient dataLakeClient;
        dataLakeClient = new DataLakeClientImpl(url);


        // create Data Lake Reader
        final DataLakeReader dataLakeReader = new DataLakeReader(dataLakeClient, monitor);
        // register Data Lake Reader
        dataOperatorRegistry.registerReader(dataLakeReader);

        // register provisioner
        @SuppressWarnings("unchecked") var retryPolicy = (RetryPolicy<Object>) context.getService(RetryPolicy.class);
        var provisionManager = context.getService(ProvisionManager.class);
        final DestinationUrlProvisioner destinationUrlProvisioner = new DestinationUrlProvisioner(dataLakeClient,
                monitor,
                retryPolicy);
        provisionManager.register(destinationUrlProvisioner);

        // register the URL resource definition generator
        var manifestGenerator = context.getService(ResourceManifestGenerator.class);
        manifestGenerator.registerGenerator(new DestinationUrlResourceDefinitionGenerator(monitor));

        // register status checker
        var statusCheckerReg = context.getService(StatusCheckerRegistry.class);
        //statusCheckerReg.register(HttpSchema.TYPE, new DataLakeStatusChecker(dataLakeClient, retryPolicy, monitor));
    }

    @Override
    public String name() {
        return "MindSphere DataLake";
    }
}
