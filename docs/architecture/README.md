**Please note**

### Work in progress

All content reflects the current state of discussion, not final decisions.

---

# Architecture

## Configuration

Each EDC extension may use its own configuration settings and should explain them in their corresponding README.md.

For a more detailed explanation of the configuration itself please see [configuration.md](configuration.md).

## Data Transfer

### Contract

Before each data transfer a contract must be offered from the provider. A consumer must negotiate an offer successfully,
before its able to request data.

These two processes (offering & negotation) are documented in the [contracts.md](contracts.md)
