# EndToEnd Transfer Test

This tests verifies some complete scenarios of contract negotiation and data transfer between a consumer and a provider.

## Modules detail

* [control-plane](./control-plane): it's responsible for handling the contract negotiation phase using in-memory persistence
* [data-plane](./data-plane): it handles the data transfer phase acting as a proxy
* [runner](./runner): it contains the test implementation

## Postgresql

To run the `EndToEndTransferPostgresqlTest` you need to spin the postgresql container:

```shell
docker run --rm --name edc-postgres -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres
```
