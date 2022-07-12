# Terminology

| Name                         | Description                                         |
|:-----------------------------|:---                                                 |
| **Artifact**                 | The instance of code which can be modulized used by the EDC
| **Asset**                    | An **asset** describes **data** that can add value to a **dataspace** and contains the **dataAddress**
| **Asset Index**              | **Asset Index**<p>* manages assets<p>* provided by an extension<p>* may support external catalogs<p>* can be queried 
| **Broker**                   | see **IDS Broker**
| **Catalog**                  | **Catalog** is a directory about all meta-information about the **assets** a **data consumer** can reach in the **dataspace**
| **Configuration**            | The EDC allows using a configuration file which will be loaded at every startup of an EDC instance
| **Connector**                | **Connector** is the central instance for a participant to join a **dataspace**. The **connector** is the gateway from the user to the **dataspace**.
| **Contract**                 | **Contract** is the finalized meta-information for a data transfer after a **contract negotiation** including the **policies** who have to be followed
| **Contract Managment**       | Includes all parts of **contract offer**, **contract agreement** and **contract negotiation**
| **Contract Agreement**       | **Contract Agreement**<p>* points to a **Contract Offer**<p>* results from a **Contract Negotiation Process**<p>* has a start date and may have a expiry date and a cancellation date
| **Contract Negotiation**     | * MVP: only possible to accept already offered contracts. Counter offers are rejected automatically.
| **Contract Offer**           | **Contract offer**<p>* set of obligations and permissions<p>* generated on the fly on provider side (see **Contract Offer Framework**)<p>* are immutable<p>* persisted in **contract negotiation process** once the negotiation has started<p>
| **Contract Offer Framework** | **Contract offer framework**<p>* generates **contract offer templates**<p>* provided by **extensions**<p>* may be implemented in custom **extensions** to created **contract offers** based on existing systems
| **Contract Offer Template**  | Blueprint of a **contract offer**
| **Consumer**                 | see **data consumer**
| **Consumer Pull**            | The data transfer is initilazied by the Consumer
| **Data**                     | **Data** describes the properties of business objects
| **Data Consumer**            | **Data consumer** are receiver of data in a data transfer
| **Data Provider**            | **Data providers** produce data and are the source for data in a data transfer
| **DataAddress**              | Reference to the physical Address of an **asset**
| **DataFlow**                 | Transfer of **data** from **data producer** to **data consumer**.
| **Dataspace**                | A **dataspace** is a space where different participants exchange **data** to get an benifit of this
| **EDC Extension**            | A module piece of the EDC which can be customized used for the EDC
| **EndpointDataReference**    | **EndpointDataReference** describes an endpoint at which one receives data
| **Event**                    | An **event** is a action which is be observed in the EDC and published by the **Event Framework**
| **Event Framework**          | **The event framework** is a way to react to actions in the EDC and can also be extended
| **Event Router**             | **Event router** is the central part of the **event framework**, which collects all published **events**
| **Extension**                | see **EDC Extension**
| **Federated Catalog**        | A decentralized **catalog**
| **Framework**                | see **contract offer framework**
| **Health**                   | Infrastructure status of the EDC
| **HTTPDataAddress**          | Specific type of **dataaddress** in context of HTTP-endpoints
| **Identity Provider**        | Management of the correct identification of all participants in the **dataspace**.
| **Injection**                | **Artifacts**/**EDC Extensions** can be injected in different parts of the EDC to be used 
| **Offer**                    | see **contract offer**
| **Offer Framework**          | see **contract offer framework**
| **Monitor**                  | The **monitor** is an interface to realise different ways to put out logs and debug information
| **Policy**                   | logical collection of rules
| **PolicyArchive**            | Location to get **policies** by their ID
| **PolicyDefinitionStore**    | Storing all **policy definitions** in the EDC
| **PolicyEngine**             | Enforcing of **policies** in the EDC
| **Provider**                 | The participant in an dataspace who is producing and publish sth. See **Data Provider**
| **Resource**                 | Infrastructure component for data transmission
| **Resource Manifest**        | Collection of **resources**
| **Rule**                     | **Rules**<p>* bound to a **contract offer**, **contract agreement** or **contract offer framework**<p>* exist independent from an **asset**
| **Transfer Process**         | **Transfer process**<p>* based on a **contract agreement**
| **Transfer Process Store**   | Storing all informations about handled **transfer processes**
| **TypeManager**              | **TypeManager** manages system types and is used to deserialize polymorphic types
| **UsageControl**             | **Usage Control** is the part of the EDC that is responsible for ensuring that the **policies** for the **data** specified in the **contract** are adhered to.
| **Vault**                    | Providing secrets