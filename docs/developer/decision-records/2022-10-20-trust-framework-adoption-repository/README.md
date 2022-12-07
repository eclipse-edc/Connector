# Trust Framework Adoption Repository

## Decision

A new repository called Adoption Framework Repository will be created in the [EDC project](https://github.com/eclipse-edc).
This repository will provide some generic extensions and documentation enabling to enforce the compliance with trust framework.
As compliance with Gaia-X was stated as a goal in the proposal on the part of the EDC project, the repository will cover the
configuration of these generic extensions in order to comply with the [Gaia-X Trust Framework](https://gaia-x.eu/wp-content/uploads/2022/05/Gaia-X-Trust-Framework-22.04.pdf).

## Rationale

Enabling sovereign and secured data exchanges between companies implies to be compliant with a set of rules.
This set of rules is known as the _trust framework_. Each trust framework defines its own rules, but they generally all fall under
certain categories, such as identity enforcement, access control... Thus, it was decided to create a dedicated repository called _Trust Framework Adoption_
which will provide generic extensions and documentation for simplifying the trust framework enforcement into the EDC components.
These extensions will be packaged and published in the same way as the other EDC components.

The Gaia-X Trust Framework is today the industry standard in terms of sovereign and trusted data exchanges, and defines
a set of rules known as the Compliance Process. Every Gaia-X certified dataspace must be compliant with this Compliance Process in order
to ensure trust and interoperability.

In order to ease adoption of the EDC into Gaia-X dataspaces, it was also decided to provide the Gaia-X implementation/configuration of the above-mentioned generic
extensions in the same repository. This approach enables to make the EDC components compliant with the Gaia-X Trust Framework without writing any code.

> ⚠️The EDC project does not aim to support all future trust frameworks in a similar form and make them available as bundles as comparable artifacts.
> Compliance with Gaia-X was stated as a goal in the proposal on the part of the EDC project and takes a separate role in the consideration.
> Thus, only the Gaia-X flavour of the above-mentioned extensions will be created and maintained under the EDC project. Any other future framework would have to develop, maintain and publish its own extensions.

## Approach

A request will be emitted to the Eclipse Foundation asking for the creation of a new repository under the [EDC project](https://github.com/eclipse-edc).
Then, all extensions and documentations related to compliance with trust framework will be placed into this repository.
