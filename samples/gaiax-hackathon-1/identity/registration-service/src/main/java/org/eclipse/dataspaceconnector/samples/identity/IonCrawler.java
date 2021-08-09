package org.eclipse.dataspaceconnector.samples.identity;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

class IonCrawler implements Crawler {
    private final Monitor monitor;

    public IonCrawler(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void run() {
        monitor.info("Crawler ran through all ION entries");
    }
}
