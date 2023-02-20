# Updating assets via Asset API

## Decision
Additional feature for Asset API that allows updating the asset's properties and data address using the Asset ID.

## Rationale

EDC does not allow for updating assets, policies, and contract definitions. The only way to make changes is to delete and recreate them, but assets can only be deleted if they are not referenced in a contract agreement. 
Updating assets is essential for effective asset management and provides more flexibility to modify and update assets easily.
For instance, If a contract has been assigned to an asset and a user discovers a typo in the asset description or wants to add additional properties to the asset, it is impossible to remove or modify this asset.

## Approach
A general approach to update an asset is as the following steps:
<ul>
<li>Send a PUT request to the Asset API endpoint for the asset, including the asset ID in the URL.</li>
<li>In the body of the request, include updated properties and data address of the asset in JSON format.</li>
<li>The server should receive the PUT request and attempt to update the entity with the given ID. If the asset does not exist, the server could create it or throw an exception.</li>
<li>After updating the entity, the server should send a response back to the client indicating whether the update was successful, and if so, the updated state of the asset.</li>
<li>Here's an example of a PUT request to update an existing user with ID "asset1":</li>

```bash

PUT /data/assets/asset1 HTTP/1.1
Content-Type: application/json

{
  "asset":{
    "properties":{
      "asset:prop:name":"Asset 1",
      "asset:prop:description":"My first asset",
      "asset:prop:AdditionalDescription":"My first asset's additional descriptions"
    }
  },
  "dataAddress":{
    "properties":{
      "baseUrl":"UPDATED_VALID_URL"
    },
    "type":"HttpData"
  }
}
```
</ul>

**Implementation:** This feature should be similar to the asset creation process, with the only difference being that only the properties and data address of the asset should be removed first and created with the new information.

**Error Handling:** Appropriate error messages will be returned to the user in case of incorrect or missing input, and data validation will be performed to ensure that the updated asset information is valid.

**Testing:** We will perform comprehensive testing to ensure that the feature is working correctly.

**Documentation:** We will update the API documentation to include information about the new feature.

