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

Awaitility permits to check multiple asynchronous behaviors at the same time with assertions that are included in the
untilAsserted block that will by default poll the assertions every 100 ms until either the assertions are valid or the
timeout expires which is by default 10 seconds.

The following are the changes in some of the tested files (before and after):

file `build.gradle.kts` (it should be the same procedure in every module)

### Class `ContractNegotiationCommandQueueIntegrationTest`

(
core\contract\src\test\java\org\eclipse\dataspaceconnector\contract\negotiation\command\ContractNegotiationCommandQueueIntegrationTest.java)

#### With CountDownLatch

```java

class ContractNegotiationCommandQueueIntegrationTest {
    //..
    @Test
    void submitTestCommand_providerManager() throws Exception {
        var negotiationManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
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
        var success = countDownLatch.await(15, TimeUnit.SECONDS);

        assertThat(success).isTrue();

        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.ERROR.code());
        assertThat(negotiation.getErrorDetail()).isEqualTo(errorDetail);

        // Stop the negotiation manager
        negotiationManager.stop();
    }
}
```

#### With awaitility

```java

class ContractNegotiationCommandQueueIntegrationTest {
    //...
    @Test
    void submitTestCommand_providerManager() {
        var negotiationManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
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

        await().untilAsserted(() -> {
            assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.ERROR.code());
            assertThat(negotiation.getErrorDetail()).isEqualTo(errorDetail);
        });

        negotiationManager.stop();
    }
}

```

### class `AssetEventDispatchTest`

(
extensions\api\data-management\asset\src\test\java\org\eclipse\dataspaceconnector\api\datamanagement\asset\service\AssetEventDispatchTest.java)

#### with CountDownLatch

```java
public class AssetEventDispatchTest {
    //...
    @Test
    void shouldDispatchEventsOnAssetCreationAndDeletion(AssetService service, EventRouter eventRouter) throws InterruptedException {
        var createdLatch = onDispatchLatch(AssetCreated.class);
        var deletedLatch = onDispatchLatch(AssetDeleted.class);
        eventRouter.register(eventSubscriber);
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("any").build();

        service.create(asset, dataAddress);

        assertThat(createdLatch.await(10, SECONDS)).isTrue();
        verify(eventSubscriber).on(isA(AssetCreated.class));

        service.delete(asset.getId());

        assertThat(deletedLatch.await(10, SECONDS)).isTrue();
        verify(eventSubscriber).on(isA(AssetDeleted.class));
    }

    private CountDownLatch onDispatchLatch(Class<? extends Event> eventType) {
        var latch = new CountDownLatch(1);

        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(eventSubscriber).on(isA(eventType));

        return latch;
    }
}
```

#### with awaitility

```java
public class AssetEventDispatchTest {
    //...
    @Test
    void shouldDispatchEventsOnAssetCreationAndDeletion(AssetService service, EventRouter eventRouter) {

        doAnswer(i -> {
            return null;
        }).when(eventSubscriber).on(isA(AssetCreated.class));

        doAnswer(i -> {
            return null;
        }).when(eventSubscriber).on(isA(AssetDeleted.class));

        eventRouter.register(eventSubscriber);
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("any").build();

        service.create(asset, dataAddress);
        service.delete(asset.getId());

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(AssetCreated.class));
            verify(eventSubscriber).on(isA(AssetDeleted.class));
        });
    }
}
```

### Additional Note:

class `TransferProcessManagerImplTest`
(core\transfer\src\test\java\org\eclipse\dataspaceconnector\transfer\core\transfer\TransferProcessManagerImplTest.java)
*** In method verifyCompleted_noCheckerForManaged(): A latch was only instantiated but not used ***