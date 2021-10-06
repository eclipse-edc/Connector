package org.eclipse.dataspaceconnector.ids.daps;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

public final class DatServiceExtensionSettings {

    @EdcSetting
    public static final String EDC_IDS_DAPS_TOKEN_URL = "edc.ids.daps.token.url";

    @EdcSetting
    public static final String EDC_IDS_DAPS_CERTIFICATE_PATH = "edc.ids.daps.certificatepath";

    @EdcSetting
    // file://tmp/key.pem
    public static final String EDC_IDS_DAPS_PRIVATEKEY_PATH = "edc.ids.daps.privatekeypath";

    public static final String EDC_IDS_DAPS_PRIVATEKEY_PASSPHRASE = "edc.ids.daps.privatekeypassphrase";

}
