**Please note**

### Work in progress

All content reflects the current state of discussion, not final decisions.

---

# IDS

## Specification

### Compliance Issues

The EDC will **not** be IDS compliant in every way. This section contains a list of issues, where the non-compliance is
a conscious decision.

##### 1. No Self-Description Response at API Root

At the root path of the API IDS requires the connector to return a self-description. This is a requirement the connector
will never fulfil. The self-description is only returned on the corresponding REST or Multipart requests.

#### 2. Only one Information Model version supported at a time

The EDC connector will not be able to support more than one IDS Information model per running instance.