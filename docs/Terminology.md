# Terminology

| Name                                   | Description                                         |
|:---                                    |:---                                                 |
| **Artifact**                           |
| **Asset**                              | **Assets**<br/>* have one content type<br/>* can be a finite as a (set of) file(s) or non finite as a service, stream</br>* are the _unit of sharing_<br/>* can point to one or more (physical) asset elements
| **Asset Element**                      |
| **Asset Index**                        | **Asset Index**<br/>* manages assets<br/>* provided by an extension<br/>* may support external catalogs<br/>* can be queried 
| **Broker**                             | see **IDS Broker**
| **Connector**                          |
| **Connector Directory**                |
| **Contract**                           |
| **Contract Agreement**                 | **Contract Agreement**<br/>* points to a **Contract Offer**<br/>* results from a **Contract Negotation Process**<br/>* has a start date and may have a expiry date and a cancellation date
| **Contract Negotiation**               |
| **Contract Offer**                     | **Contract Offer**<br/>* set of obligations and permissions<br/>* can be generated on the fly on provider side (see **Contract Offer Framework**)<br/>* are immutable<br/>* persisted in **Contract Negotiation Process** once the negotiation has startet<br/>* updated versions during the **Contract Negotiation Process** may be generated manually<br/>* target of rules<br/>
| **Contract Offer Framework**           | **Contract Offer Framework**<br/>* generates **Contract Offers**<br/>* refers to 0..n **Consumers** and to 1..n **Assets**<br/>* provided by an **Extension**<br/>* can be based on "pre-established agreements"
| **Consumer**                           |
| **Dataspace**                          |
| **EDC Extension**                      |
| **Element**                            | see **Asset Element**
| **Extension**                          | see **EDC Extension**
| **Framework**                          | see **Contract Offer Framework**
| **Identity Provider**                  |
| **IDS Broker**                         | IDS version of the **Connector Directory**
| **Offer**                              | see **Contract Offer**
| **Offer Framework**                    | see **Contract Offer Framework**
| **Provider**                           |
| **Resource**                           |
| **Resource Manifest**                  |
| **Rule**                               | **Rules**<br/>* bound to a **Contract Offer**, **Contract Agreement** or **Contract Offer Framework**<br/>* exist independent from an **Asset**
| **Transfer Process**                   | **Transfer Process**<br/>* based on a **Contract Agreement**