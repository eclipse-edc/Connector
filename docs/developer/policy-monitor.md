# Policy Monitor

The Policy Monitor is one EDC connector module that permits to continuously enforce policies on running Transfer Processes.

## Overview

Not finite transfers like streaming flows, that are started but, in fact, technically they don't complete, need to be
under control to ensure that they don't continue transferring data also when the contract policy is not valid anymore.

The Policy Monitor takes care of that, it's implemented as a State Machine, that starts when a new provider Transfer Process
gets started, continuously loop and evaluate the contract policy using the Policy Engine.
In the very moment that the Policy is not valid anymore, the Policy Monitor completes the Transfer Process.

## Embedded deployment
To deploy the Policy Monitor embedded in the control-plane requires to add the `policy-monitor-core` extension
to the bundle.

The Policy Monitor state machine can be configured in the same way other state machines are configured, using these properties:
```
edc.policy.monitor.state-machine.iteration-wait-millis
edc.policy.monitor.state-machine.batch-size
```

Take a look at the [performance tuning page](performance-tuning.md) for further details.

## Standalone deployment

[Not implemented yet](https://github.com/eclipse-edc/Connector/issues/3446)
