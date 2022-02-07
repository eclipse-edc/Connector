package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model;

public interface Transformer<DTO, DOMAINOBJECT> {

    DTO convertToDto(DOMAINOBJECT domainobject);

    DOMAINOBJECT convertToObject(DTO dto);
}
