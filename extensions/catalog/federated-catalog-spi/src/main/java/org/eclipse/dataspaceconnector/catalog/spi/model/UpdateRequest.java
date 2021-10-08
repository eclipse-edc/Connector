package org.eclipse.dataspaceconnector.catalog.spi.model;


public class UpdateRequest {
    private final String nodeUrl;

    public UpdateRequest(String nodeUrl) {

        this.nodeUrl = nodeUrl;
    }
}
