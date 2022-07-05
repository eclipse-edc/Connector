# Guideline for submitting features

This document is intended as guideline for contributors who either already have implemented a feature, e.g. an extension, or intend to do so, and are looking for ways to upstream that feature into the EDC.

There are currently two possible levels of adoption for the EDC project:
1. incorporate a feature as core EDC component
2. reference a feature as "friend" 

## Get referenced as "friend"

This means we will add a link to our [known friends](known_friends.md) list, where we reference projects and features that we are aware of. These are repositories that have no direct affiliation with EDC and are hosted outside of the `eclipse-dataspaceconnector` GitHub organization. We call this a "friend" of EDC (derived from the C++ [`friend class` concept](https://en.cppreference.com/w/cpp/language/friend)). 
In order to become a "friend" of EDC, we do a quick scan of the code base to make sure it does not contain anything offensive, or that contradicts our code of conduct, ethics or other core OSS values. 

The EDC core team does not maintain or endorse "friend" projects in any way, nor is it responsible for it, but we do provide a URL list to make it easier for other developers to find related projects and get an overview of the EDC market spread.

This is the easiest way to "get in" and will be the suitable form of adoption for _most_ features and projects.

## Get adopted in EDC core

This means the contribution gets added to the EDC code base, and is henceforth maintained by the EDC core team. The barrier of entry for this is much higher than for "friends", and a more in-depth review of the code will be performed. 

Note that this covers both what we call the [EDC Core repository](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector) as well as any current or future repositories in the `eclipse-dataspaceconnector` GitHub organization.
It is up to the committers to decide where the code will eventually be hosted in case of adoption.

However, in order to do a preliminary check, please go through the following bullet points:
#### Why should this contribution be adopted?
Please argue why this feature must be hosted upstream and be maintained by the EDC core team.
#### Could it be achieved with existing functionality? If not, why?
If there is any existing code that can achieve the same thing with little modification, that is usually the preferable way for the EDC core team. We aim to keep the code succinct and want to avoid similar/duplicate code. Make sure you understand the EDC code base well!
#### Are there multiple use cases or applications who will benefit from the contribution?
Basically, we want you to motivate who will use that feature and why, thereby arguing the fact that it is well-suited to be adopted in the core code base. One-off features are better suited to be maintained externally. 
#### Can it be achieved without introducing third-party dependencies? If not, which ones?
EDC is a platform rather than an application, therefore we are extremely careful when it comes to introducing third party libraries. The reasons are diverse: security, license issues and over all JAR weight, just to mention a few important ones.
#### Would this feature limit platform independence in any way? If so, how and why?
Features that do not work well in clustered environments are difficult to adopt, since EDC is designed from the ground up to be stateless and clusterable. Similarly, features, that have dependencies onto certain operating systems are difficult to argue.
#### Is it going to be a self-contained feature, or would it cut across the entire code base?
Features that have a large impact on the code base are very complex to thoroughly test, they have a high chance to destabilize the code and require careful inspection. Self-contained features on the other hand are easier to isolate and test.

And on a more general level:
- does your contribution comply with our [licensing](LICENSE)?
- does the code adhere to our [styleguide](styleguide.md) and our [architectural principles](docs/architecture/architecture-principles.md)?
- are you willing to accept our [contributing guidelines](CONTRIBUTING.md)?
- are you prepared to make frequent contributions and help out with maintaining this feature?

When you submit an application for adopting a feature, _be prepared to answer all of them in an exhaustive and coherent way_!

Note that even if all of the aforementioned points are answered satisfactorily, **the EDC core team reserves the right to ultimately decide whether a feature will get adopted or not.** 

## Submitting an application

Please open in issue using the [Adoption request](.github/ISSUE_TEMPLATE/adoption_request.md) template, fill out all the sections to the best of your knowledge and wait to hear back from the EDC core team. We will comment in the issue, or reach out to you directly. Be aware that omitting sections from the application will greatly diminish the chance of approval.
