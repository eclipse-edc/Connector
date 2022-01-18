package org.eclipse.dataspaceconnector.samples.sample042;

import org.eclipse.dataspaceconnector.core.system.runtime.BaseRuntime;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;

public class SampleRuntime extends BaseRuntime {

    private TransferProcessStore store;

    public static void main(String[] args) {
        SampleRuntime runtime = new SampleRuntime();
        runtime.boot();

    }


}
