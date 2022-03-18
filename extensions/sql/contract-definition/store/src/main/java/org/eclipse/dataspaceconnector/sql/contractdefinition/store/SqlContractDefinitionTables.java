package org.eclipse.dataspaceconnector.sql.contractdefinition.store;

public interface SqlContractDefinitionTables {
    String CONTRACT_DEFINITION_TABLE = "edc_contract_definitions";
    String CONTRACT_DEFINITION_COLUMN_ID = "contract_definition_id";
    String CONTRACT_DEFINITION_COLUMN_ACCESS_POLICY = "access_policy";
    String CONTRACT_DEFINITION_COLUMN_CONTRACT_POLICY = "contract_policy";
    String CONTRACT_DEFINITION_COLUMN_SELECTOR = "selector_expression";
}
