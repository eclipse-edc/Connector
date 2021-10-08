package org.eclipse.dataspaceconnector.catalog.spi;


public interface LoaderManager {

    String FEATURE = "edc:catalog:cache:loadermanager";

    void start();

    void stop();

    void addLoader(Loader loader);
}
