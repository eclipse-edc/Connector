# Onboarding and first steps

This document serves as step-by-step guide for new community members and assumes a working knowledge of the Eclipse
Dataspace Connector (EDC) nomenclature. If you do not know about the EDC nomenclature we strongly advise reading the
documentation and/or watch the introductory video.

All chapters of this guide are incremental, so e.g. example 3 uses code from example 2. All code resides in
the [`samples`](./samples) directory of this repository.

Also, a working knowledge of Git, Gradle, Java and HTTP is presumed.

We'll assume that you've just checked out the EDC code base and have Java 11 installed on your development machine. 
If not, please download and install JDK 11 for your OS.

Command examples in this document will use the `bash` syntax and use Unix-style paths, but any other shell should be
fine as well. If you're using Windows you either need to adapt the paths or use WSL2.

Please simply follow the following links to the respective samples.

> _Please note that the entire Eclipse Dataspace Connector is under heavy development, so things are likely to change significantly
> in the future. Be sure to check back regularly to stay updated!_

## [Example 1: Run a simple connector](samples/01-basic-connector/README.md)

> _The complete sample code for this chapter is in `samples/01-basic-connector/`._

## [Example 2: Write your first extension](samples/02-health-endpoint/README.md)

> _The complete sample code for this chapter is in `samples/02-health-endpoint`._

## [Example 3: Use the file-system based configuration](samples/03-configuration/README.md)

> _The complete sample code for this chapter is in `samples/03-configuration`._

## [Example 4.0: Implement a simple file transfer](samples/04.0-file-transfer/README.md)

> _The complete sample code for this chapter is in `samples/04.0-file-transfer`._

## [Example 4.1: Implement a simple file transfer listener](samples/04.1-file-transfer-listener/README.md)

> _The complete sample code for this chapter is in `samples/04.1-file-transfer-listener`._

## [Example 4.2: Modify a TransferProcess](samples/04.2-modify-transferprocess/README.md)

> _The complete sample code for this chapter is in `samples/04.2-modify-transferprocess`._

## [Example 5: Improve the file transfer](samples/05-file-transfer-cloud/README.md)

> _The complete sample code for this chapter is in `samples/05-file-transfer-cloud`._
