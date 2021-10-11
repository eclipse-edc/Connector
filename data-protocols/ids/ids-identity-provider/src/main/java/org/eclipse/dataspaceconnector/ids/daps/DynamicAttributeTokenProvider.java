package org.eclipse.dataspaceconnector.ids.daps;

public interface DynamicAttributeTokenProvider {

    DynamicAttributeToken getDynamicAttributeToken() throws DynamicAttributeTokenException;
}
