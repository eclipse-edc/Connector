package org.eclipse.dataspaceconnector.transfer.dataplane.sync;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

import java.util.concurrent.TimeUnit;

interface DataPlaneTransferSyncConfiguration {

    String API_CONTEXT_ALIAS = "validation";

    @EdcSetting
    String DATA_PLANE_PUBLIC_API_ENDPOINT = "edc.transfer.dataplane.sync.endpoint";

    @EdcSetting
    String DATA_PLANE_PUBLIC_API_TOKEN_VALIDITY_SECONDS = "edc.transfer.dataplane.sync.token.validity";
    long DEFAULT_DATA_PLANE_PUBLIC_API_TOKEN_VALIDITY_SECONDS = TimeUnit.MINUTES.toSeconds(10);

    @EdcSetting
    String PUBLIC_KEY_ALIAS = "edc.public.key.alias";
}
