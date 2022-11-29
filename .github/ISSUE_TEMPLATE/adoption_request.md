---
name: Adoption Request
about: Submit your feature to the project
title: 'Adoption Request'
labels: 'adoption'
assignees: ''

---

# Adoption Request

_Thank you for wanting to contribute to the project! We are very happy to see the functionalities of the EDC being extended. Providing this open-source is a great opportunity for others with similar requirements and to avoid additional work._

_For any details about the guidelines for submitting features, please take a look at the [contribution categories](https://github.com/eclipse-edc/Connector/blob/main/contribution_categories.md)._


## General information

Please provide some information about your project or code contribution. 

_If you choose to be referenced as a "friend", these will be added to the [known friends list](https://github.com/eclipse-edc/Connector/blob/main/known_friends.md)._
_If you choose to add your feature as a core EDC component, links to your current code and correlated issues, discussions, and pull requests are of great importance._

| Title | Description | Contact | Links
| :--- | :--- | :--- | :---
| _My awesome project_ | _This is an example._ | _e-mail-address_ | _link to repository, homepage, discussion, etc._


## Adoption level

Next, please tell us what level of adoption you intend to provide. _(pick only one)_

- [ ] Reference a feature as "friend"
- [ ] Incorporate a feature as core EDC component



## Adoption in EDC core

_If you chose to add your feature as a core EDC component, please answer the following questions._

### Why should this contribution be adopted?
_Please argue why this feature must be hosted upstream and be maintained by the EDC core team._

### Could it be achieved with existing functionality? If not, why?
_If there is any existing code that can achieve the same thing with little modification, that is usually the preferable way for the EDC core team. We aim to keep the code succinct and want to avoid similar/duplicate code. Make sure you understand the EDC code base well!_

### Are there multiple use cases or applications who will benefit from the contribution?
_Basically, we want you to motivate who will use that feature and why, thereby arguing the fact that it is well-suited to be adopted in the core code base. One-off features are better suited to be maintained externally._

### Can it be achieved without introducing third-party dependencies? If not, which ones?
_EDC is a platform rather than an application, therefore we are extremely careful when it comes to introducing third party libraries. The reasons are diverse: security, license issues and over all JAR weight, just to mention a few important ones._

### Would this feature limit platform independence in any way? If so, how and why?
_Features that do not work well in clustered environments are difficult to adopt, since EDC is designed from the ground up to be stateless and clusterable. Similarly, features, that have dependencies onto certain operating systems are difficult to argue._

### Is it going to be a self-contained feature, or would it cut across the entire code base?
_Features that have a large impact on the code base are very complex to thoroughly test, they have a high chance to destabilize the code and require careful inspection. Self-contained features on the other hand are easier to isolate and test._
