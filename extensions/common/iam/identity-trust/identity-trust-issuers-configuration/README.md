# Trusted Issuer Configuration Extension

This IATP extension makes it possible configure a list of trusted issuers, that will be used matches against the Verifiable Credential issuers.

## Configuration

Per issuer the following settings must be configured. As `<issuer-alias>` any unique string is valid.

| Key                                                  | Description                      | Mandatory | 
|:-----------------------------------------------------|:---------------------------------|-----------|
| edc.iam.trusted-issuer.``<issuer-alias>``.id         | ID of the issuer.                | X         |
| edc.iam.trusted-issuer.``<issuer-alias>``.properties | Additional properties of Issuer. | (X)       | 

