# Instructions to run a file-transfer example locally

In this tutorial, we will deploy 2 eclipse dataspace connectors (EDC): a provider EDC that shares a file, and a consumer EDC that acquires the file. 

## Prerequisites

- Docker
- Postman
- 2 Pre-signed URLs (Java code to generate [pre-signed URLs for AWS S3](https://code.siemens.com/-/snippets/2984)):
    1. Pre-signed URL for download (for HTTP GET)
    1. Pre-signed URL for upload (for HTTP PUT)

## Important Notes

- The configuration files of the provider EDC (i.e., `build.gradle.kts`, and `config.properties`) are located in `DataSpaceConnector-Fork/samples/other/file-transfer-http-to-http/provider/`.
- Similarly, the configuration files of the EDC consumer (i.e., `build.gradle.kts`, and `config.properties`) are in `DataSpaceConnector-Fork/samples/other/file-transfer-http-to-http/consumer/`.
- In case the connectors are deployed without the provided `docker-compose.yml` file (see section Deployment), make sure to adjust the webhood addresses (inside `config.properties`) of both the provider EDC and the consumer EDC. Otherwise, the file transfer will not work.

## Deployment

1. Clone/download the repository.
1. Open a Terminal window and browse to `DataSpaceConnector-Fork/launchers/mdsp-connector/`
1. Run `docker-compose up &`. It will take about 10-15 minutes to build the containers.

## File-transfer example

1. Start Postman and import the collection `DataSpaceConnector-Fork/samples/other/file-transfer-http-to-http/postman/catenax-edc-local.postman_collection.json`.
1. In the folder Provider, we can do the following requests:

    1. Health check: To check if the provider is alive.
    1. Create asset: To create an asset for the file that we want to share. Edit the following:
        - `asset:prop:id`: The asset ID. Let this ID = 2 for this example.
        - `asset:prop:name`: The file name.
        - `url`: The address of the file. Add here the pre-signed URL for download (see Prerequisites section).
    1. Get all assets: To list all the assets. The asset we created has ID = 2.
    1. Create policy: To create a new policy for our asset. Do not modify for this example.
    1. Get all policies: To list all the policies.
    1. Create Contract: To create a new contract for our asset. Do not modify for this example.
    1. Get all contracts: To list all the contracts. The contract we created has ID = 2 and the contractPolicyId that we put in step 4.

1. In the folder Consumer, we can do the following requests:

    1. Health check: To check if the consumer is alive.
    1. Get offers: To list the assets of the provider EDC. In the reponse, find the contract offer that has `asset:prop:id` = 2 (which we created earlier), and take a note of the contract offer `id`, and the policy `uid`.
    1. Negotiate contract: To request the asset from the provider.
        1. Copy the contract offer `id` from the previous step to the field `offerId`.
        1. Copy the policy `uid` from the previous step to the field `uid` of the policy.
        1. After sending this request, take a note of the negotiation `id` which is included in the response.
    1. Get contract ID: To get the contract agreement ID, modify the URL of this request, and add the negotiation `id` from the previous step, i.e., `http://localhost:9192/api/v1/data/contractnegotiations/{negotiation id}`. In the response, take a note of the `contractAgreementId`.
    1. Inititate transfer: To start the file transfer. The process to tranfer the file usually takes few seconds (depends also on the file size).
        - Change the `contractId` to the `contractAgreementId` from the previous step.
        - Change the `keyName` to the file name.
        - Change the `url` to the pre-signed upload URL (see prerequisites section).
    1. Get status of all transfers: To check the status of all the transfers. 





