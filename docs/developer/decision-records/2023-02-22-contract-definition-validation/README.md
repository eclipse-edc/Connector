
# Contract Definition Validation

## Decision

Remove the `ContractDefinition` validation check on `ContractValidationServiceImpl#validateAgreement`. The validation of the `ContractDefinition` policies
should only be done when negotiating a new contract.

## Rationale

Data providers may want to change the conditions on which assets are offered, or they simply don't want to expose those data anymore for future contracts. 
For that `ContractDefinition` can be deleted via `DataManagement` APIs but currenty the `ContractDefinition` is checked not only when negotiating a new `ContractAgreement`, but also when validating an existing one. 
This leads to errors for example on transfer requests where the contract agreement reference a contract definition that has been deleted.

## Approach

Since all the information for validating an agreement are in the agreement itself, we can just remove:

```java
var contractDefinition = contractDefinitionService.definitionFor(agent, contractId.definitionPart());
if (contractDefinition == null) {
    return Result.failure(format("The ContractDefinition with id %s either does not exist or the access to it is not granted.", agreement.getId()));
}
```

from the method `ContractValidationServiceImpl#validateAgreement`.
