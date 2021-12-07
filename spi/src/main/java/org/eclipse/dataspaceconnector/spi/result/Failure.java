package org.eclipse.dataspaceconnector.spi.result;

import java.util.List;

public interface Failure {
    List<String> getMessages();
}
