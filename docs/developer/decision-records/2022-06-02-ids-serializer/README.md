# IDS Serializer Implementation

## Decision

This decision record documents the decision on _not_ integrating the currently still most widely used [IDS Infomodel
Serializer Java Library](https://maven.iais.fraunhofer.de/ui/native/eis-ids-public/de/fraunhofer/iais/eis/ids/infomodel-serializer/). 
The EDC will provide an own implementation to deserialize IDS JSON-LD strings and convert them into Java objects. 
With this, there is a first deviation from previously used Java libraries.

## Rationale

The usage of the [IDS Information Model](https://github.com/International-Data-Spaces-Association/InformationModel) 
is not under discussion, as the EDC is supposed to be compatible with the IDS architecture and communication protocols. 
Nevertheless, the way how the IDS Information Model is technically implemented is, as of now, not predefined.

Currently, most IDS connector reference implementations are using the Java libraries that are provided via
the [Fraunhofer IAIS Maven Artifactory](https://maven.iais.fraunhofer.de). This includes, i. a., the
[IDS Information Model Serializer](https://maven.iais.fraunhofer.de/ui/native/eis-ids-public/de/fraunhofer/iais/eis/ids/infomodel-serializer/).
With the help of this library, incoming JSON-LD strings with IDS content can be checked for their syntactical 
correctness before being converted into Java objects for further processing.

Up to this point, the IDS Serializer was not used in the EDC, instead a custom implementation (using
`com.fasterxml.jackson.databind.ObjectMapper`) was created to process incoming IDS messages. The following 
table compares pros and cons of continuing with a separate implementation versus using the existing IDS Serializer Library.

|  | Own Implementation | Usage of IDS Serializer Lib |
|:---|:---|:---|
| maintenance | same codebase, same concepts/principles | no know-how, no responsibles in the EDC community, pending familiarization
| dependencies | minimal dependencies on external libraries | existing dependencies on external libraries
| code quality | same codebase, same concepts/principles, automated style checks etc. | concurrency problems (not thread-safe)
| up-to-date | anyway waiting for IDS Infomodel 5.0, initial implementation already available (ObjectMapper) | outdated development status of the publicly visible source code
| security | same codebase, security checks | currently no security patches
| ids interoperability (syntax) | yet to be established | given as existing IDS Java libraries are used by other connector implementations
| current usage | -- | usage of Maven artifacts generated from a private IAIS repository, no usage of the IDS Serializer in the EDC organisation
| effort | low, due to few agreements, initial implementation (ObjectMapper) available | IP checks, revision of existing (few) classes

## Approach

The further actions should be as follows:
1. The current object mapper capabilities with IDS modifications will be converted into an extension.
2. In order to use the IDS Serializer repository within the EDC organization, the extension will be moved there.
3. As result, the EDC's IDS Serializer can be uploaded as EDC maven artifact (e.g., to Maven Central), used as a 
dependency, and further improved and modified as necessary.
