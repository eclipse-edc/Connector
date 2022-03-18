# IDS Token Validation

Extends the default OAuth2 token validation with more specific IDS token validation rules.

## Additonal Information needed

Requires additional IDS specific information for the validation rules from the IDS messages which are not part of the token:
- securityProfile
- issuerConnector

## Configuration

| Key |  Description |
|:---|:---|
| edc.ids.validation.referringconnector | Validate DAT referringConnector vs IDS-Message issuerConnector (true/false, default false [not enabled]) |
