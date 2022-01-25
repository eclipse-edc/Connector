package org.eclipse.dataspaceconnector.samples.sample042;

import org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime;

public class SampleRuntime extends BaseRuntime {
    
    public static void main(String[] args) {
        SampleRuntime runtime = new SampleRuntime();
        runtime.boot();
    }
}
