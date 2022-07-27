# Unify asynchronous code testing practices

## Decision

Currently, the testing of async code is performed in two different ways:

* using the `CoundDownLatch` object
* using the `Awaitility` library

The goal of `CountDownLatch` objects and the `Awaitility` library is to cause a thread to block until other threads have
completed a given task. Something useful in order to test Asynchronous threads.

The idea is to avoid using `CoundDownLatch` and start only using `Awaitility`.

## Rationale

Testing asynchronous systems is difficult. It requires not only handling threads, timeouts and concurrency issues, but
also the intent of the test code can be obscured by all these details e.g while using CountDownLatch. Awaitility is a
DSL that allows to express expectations of an asynchronous system in a conciser and easier to read manner. Additionally
CoundDownLatch works only based on latches while Awaitility works based on time (seconds, nanoseconds), which can be
more useful to do time testing on threads.

## Approach

Adding the dependency Awaitility into every build.gradle.kts

```java
...
        dependencies{
        ...
        testImplementation("org.awaitility:awaitility:${awaitility}")
        }
        ...
```

Class `AzureVaultExtensionTest`
(extensions\azure\vault\src\test\java\org\eclipse\dataspaceconnector\core\security\azure\AzureVaultExtensionTest.java)

Since it is possible to set the waiting timeout on `Waitility` the tests instead of testing time, now check that the
methods are working correctly.

```java
@Test
    void onlyCertificateProvided_authenticateWithCertificate()throws InterruptedException{
            when(context.getSetting("edc.vault.certificate",null)).thenReturn(CERTIFICATE_PATH);

            try(MockedStatic<AzureVault> utilities=mockStatic(AzureVault.class)){
        utilities.when(()->AzureVault.authenticateWithCertificate(monitor,CLIENT_ID,TENANT_ID,CERTIFICATE_PATH,KEYVAULT_NAME))
        .then(i->{
        await().timeout(Duration.ofMillis(TIMEOUT_MS));
        return null;
        });
        extension.initialize(context);
        }
        }

@Test
    void onlySecretProvided_authenticateWithSecret()throws InterruptedException{
            when(context.getSetting("edc.vault.clientsecret",null)).thenReturn(CLIENT_SECRET);

            try(MockedStatic<AzureVault> utilities=mockStatic(AzureVault.class)){
        utilities.when(()->AzureVault.authenticateWithSecret(monitor,CLIENT_ID,TENANT_ID,CLIENT_SECRET,KEYVAULT_NAME))
        .then(i->{
        await().timeout(Duration.ofMillis(TIMEOUT_MS));
        return null;
        });

        extension.initialize(context);
        }
        }

@Test
    void bothSecretAndCertificateProvided_authenticateWithCertificate()throws InterruptedException{
            when(context.getSetting("edc.vault.certificate",null)).thenReturn(CERTIFICATE_PATH);
            when(context.getSetting("edc.vault.clientsecret",null)).thenReturn(CLIENT_SECRET);

            try(MockedStatic<AzureVault> utilities=mockStatic(AzureVault.class)){
        utilities.when(()->AzureVault.authenticateWithCertificate(monitor,CLIENT_ID,TENANT_ID,CERTIFICATE_PATH,KEYVAULT_NAME))
        .then(i->{
        await().timeout(Duration.ofMillis(TIMEOUT_MS));
        return null;
        });
        extension.initialize(context);
        }
        }
```

Class `LockManagerTest`
(common\util\src\test\java\org\eclipse\dataspaceconnector\common\concurrency\LockManagerTest.java)

Same as with the previous class, the same timeout is set and it is tested only the correct flow of the code.

```java
...
@Test
    void verifyTimeoutOnWriteLockAttempt(){
            var lockManager=new LockManager(new ReentrantReadWriteLock(),10);
            var counter=new AtomicInteger();

            // Attempt to acquire a write lock in another thread, which should timeout as the current thread holds a read lock
            var thread=new Thread(()->{
            try{
            lockManager.writeLock(()->{
            throw new AssertionError();  // lock should never be acquired
            });
            }catch(LockException e){
            await().timeout(Duration.ofMillis(1000L));
            }
            });

            lockManager.readLock(()->{
            thread.start();
            counter.incrementAndGet();
            return null;
            });

            assertThat(counter.get()).isEqualTo(1);
            }
            ...

```

Class `ContractNegotiationCommandQueueIntegrationTest`
(
core\contract\src\test\java\org\eclipse\dataspaceconnector\contract\negotiation\command\ContractNegotiationCommandQueueIntegrationTest.java)

Instead of evaluating a latch that lasts for 15 seconds, an object await is created with a duration of 15 seconds and
the methods getState and getErrorDetail are the only ones evaluated.

```java
...
@Test
    void submitTestCommand_providerManager()throws Exception{
            var negotiationManager=ProviderContractNegotiationManagerImpl.Builder.newInstance()
            .monitor(monitor)
            .validationService(validationService)
            .dispatcherRegistry(dispatcherRegistry)
            .commandQueue(commandQueue)
            .commandRunner(commandRunner)
            .observable(observable)
            .store(store)
            .policyStore(policyStore)
            .sendRetryManager(sendRetryManager)
            .build();

            negotiationManager.start();

            negotiationManager.enqueueCommand(command);

            // Wait for CommandHandler to modify negotiation with time out at 15 seconds
            await().timeout(java.time.Duration.ofSeconds(15));

            assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.ERROR.code());
            assertThat(negotiation.getErrorDetail()).isEqualTo(errorDetail);

            // Stop the negotiation manager
            negotiationManager.stop();
            }

@Test
    void submitTestCommand_consumerManager()throws Exception{
            when(store.find(negotiationId)).thenReturn(negotiation);

            // Create and start the negotiation manager
            var negotiationManager=ConsumerContractNegotiationManagerImpl.Builder.newInstance()
            .monitor(monitor)
            .validationService(validationService)
            .dispatcherRegistry(dispatcherRegistry)
            .commandQueue(commandQueue)
            .commandRunner(commandRunner)
            .observable(observable)
            .store(store)
            .policyStore(policyStore)
            .sendRetryManager(sendRetryManager)
            .build();
            negotiationManager.start();

            // Enqueue command
            negotiationManager.enqueueCommand(command);

            // Wait for CommandHandler to modify negotiation with time out at 15 seconds
            await().timeout(java.time.Duration.ofSeconds(15));

            assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.ERROR.code());
            assertThat(negotiation.getErrorDetail()).isEqualTo(errorDetail);

            // Stop the negotiation manager
            negotiationManager.stop();
            }
            ...
```

class `TransferProcessManagerImplIntegrationTest`
(
core\transfer\src\test\java\org\eclipse\dataspaceconnector\transfer\core\transfer\TransferProcessManagerImplIntegrationTest.java)
***No tests found for given includes***

class `TransferProcessManagerImplTest`
(core\transfer\src\test\java\org\eclipse\dataspaceconnector\transfer\core\transfer\TransferProcessManagerImplTest.java)
*** method verifyCompleted_noCheckerForManaged(): A latch was only instantiated but not used ***

class `AssetEventDispatchTest`
(
extensions\api\data-management\asset\src\test\java\org\eclipse\dataspaceconnector\api\datamanagement\asset\service\AssetEventDispatchTest.java)

Instead of creating and testing latches which lasts for 10 seconds, an timeout is set and it is only the events of
creation and deletion of assets are tested

```java
...
@Test
    void shouldDispatchEventsOnAssetCreationAndDeletion(AssetService service,EventRouter eventRouter)throws InterruptedException{

            eventRouter.register(eventSubscriber);
            var asset=Asset.Builder.newInstance().id("assetId").build();
            var dataAddress=DataAddress.Builder.newInstance().type("any").build();

            service.create(asset,dataAddress);
            await().timeout(Duration.ofSeconds(10));

            verify(eventSubscriber).on(isA(AssetCreated.class));

        service.delete(asset.getId());
        await().timeout(Duration.ofSeconds(10));

        verify(eventSubscriber).on(isA(AssetDeleted.class));
        }
```

class `PolicyDefinitionEventDispatchTest`
(
extensions\api\data-management\policydefinition\src\test\java\org\eclipse\dataspaceconnector\api\datamanagement\policy\service\PolicyDefinitionEventDispatchTest.java)

the same procedure as the previous class applies to this tests

```java
...
@Test
    void shouldDispatchEventOnPolicyDefinitionCreationAndDeletion(PolicyDefinitionService service,EventRouter eventRouter)throws InterruptedException{
            eventRouter.register(eventSubscriber);
            var policyDefinition=PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();

            service.create(policyDefinition);
            await().timeout(Duration.ofSeconds(10));
            verify(eventSubscriber).on(isA(PolicyDefinitionCreated.class));

        service.deleteById(policyDefinition.getUid());
        await().timeout(Duration.ofSeconds(10));
        verify(eventSubscriber).on(isA(PolicyDefinitionDeleted.class));
        }
```

extensions\junit\src\main\java\org\eclipse\dataspaceconnector\junit\extensions\EdcRuntimeExtension.java

common\state-machine-lib\src\test\java\org\eclipse\dataspaceconnector\common\statemachine\StateMachineManagerTest.java
extensions\catalog\federated-catalog-cache\src\test\java\org\eclipse\dataspaceconnector\catalog\cache\crawler\CrawlerImplTest.java
extensions\catalog\federated-catalog-cache\src\test\java\org\eclipse\dataspaceconnector\catalog\cache\loader\LoaderManagerImplTest.java
--> 1 test good, 1 test bad
extensions\catalog\federated-catalog-cache\src\test\java\org\eclipse\dataspaceconnector\catalog\cache\management\PartitionManagerImplIntegrationTest.java
extensions\catalog\federated-catalog-cache\src\test\java\org\eclipse\dataspaceconnector\catalog\cache\management\PartitionManagerImplTest.java
extensions\data-plane\data-plane-framework\src\test\java\org\eclipse\dataspaceconnector\dataplane\framework\manager\DataPlaneManagerImplTest.java

--> Not test but main folder

extensions\http-provisioner\src\test\java\org\eclipse\dataspaceconnector\transfer\provision\http\impl\HttpProvisionerExtensionEndToEndTest.java