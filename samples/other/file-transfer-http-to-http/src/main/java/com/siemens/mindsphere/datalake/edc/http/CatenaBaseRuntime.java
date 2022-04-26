package com.siemens.mindsphere.datalake.edc.http;

import org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime;

public class CatenaBaseRuntime extends BaseRuntime {

    /**
     * The {@code main} method must be re-implemented, otherwise {@link BaseRuntime#main(String[])} would be called, which would
     * instantiate the {@code BaseRuntime}.
     */
    public static void main(String[] args) {
        new CatenaBaseRuntime().boot();
    }
}
