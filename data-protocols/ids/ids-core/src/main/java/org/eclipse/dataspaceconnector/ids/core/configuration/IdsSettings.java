package org.eclipse.dataspaceconnector.ids.core.configuration;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

public final class IdsSettings {

    @EdcSetting
    public static final String EDC_IDS_DAPS_TOKEN_URL = "edc.ids.daps.tokenurl";

    @EdcSetting
    public static final String EDC_IDS_DAPS_CERTIFICATE_PATH = "edc.ids.daps.certificatepath";

    @EdcSetting
    public static final String EDC_IDS_DAPS_PRIVATEKEY_PATH = "edc.ids.daps.privatekeypath";

    @EdcSetting
    public static final String EDC_IDS_DAPS_PRIVATEKEY_PASSPHRASE = "edc.ids.daps.privatekeypassphrase";

}
