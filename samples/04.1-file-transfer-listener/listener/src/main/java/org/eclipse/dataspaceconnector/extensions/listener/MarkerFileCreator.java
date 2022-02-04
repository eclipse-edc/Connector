package org.eclipse.dataspaceconnector.extensions.listener;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessListener;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;

public class MarkerFileCreator implements TransferProcessListener {

    private final Monitor monitor;

    public MarkerFileCreator(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Callback invoked by the EDC framework when a transfer has completed.
     *
     * @param process the transfer process that has completed.
     */
    @Override
    public void completed(final TransferProcess process) {
        Path path = Path.of(process.getDataRequest().getDataDestination().getProperty("path"));
        if (!Files.isDirectory(path)) {
            path = path.getParent();
        }
        path = path.resolve("marker.txt");

        try {
            Files.writeString(path, "Transfer complete");
            monitor.info(format("Transfer Listener successfully wrote file %s", path));
        } catch (IOException e) {
            monitor.warning(format("Could not write file %s", path), e);
        }
    }
}
