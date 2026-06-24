# Lib Module Consolidation

## Decision

As a companion to the SPI consolidation (see `2026-06-18-spi-module-consolidation`), we will consolidate the ~43
`*-lib` modules by collapsing over-granular clusters and tiny 1:1 dependency chains into a smaller set of
domain-cohesive libraries.

Unlike the SPI modules, the `lib` modules are _implementation_ libraries — reusable code that registers no extensions —
so this is not an audience-based re-tiering. It is the removal of module boundaries that no longer need to exist. The
notable merges are:

- **`dsp-lib`** — collapse the 13 modules under `data-protocols/dsp/dsp-lib` (the `{catalog, negotiation,
  transfer-process} × {http-api, transform, validation}` matrix, plus `dsp-version-transform-lib`, plus three empty
  umbrella modules) into a single library.
- **`jsonld-lib`** — the JSON / JSON-LD / transform / validation serialization stack.
- **`core-lib`** — all library code that is _generic_ as in: not specific to a connector.
- **`control-plane-lib`** - all library code that is specific to a connector, such as policy engine and evaluator,
  stores, state machines, etc.

We accept that this removes published Maven coordinates and is therefore a breaking change for downstream adopters that
depend on the individual modules directly. We also accept that in some instances adopters will have superfluous code on
their classpath.

## Rationale

The same forces that motivated the SPI consolidation apply to the `lib` tree, which has grown to ~43 modules:

- Many libraries are tiny (several contain a single class or a handful), split along historical lines, and are only ever
  consumed together.
- The split adds build, wiring, and publishing overhead for no separation-of-concerns benefit — publishing every module
  individually to Maven Central is a large part of the ~2:45h publish time we are currently experiencing.
- The `data-protocols/dsp/dsp-lib` tree is the clearest case: a 3×3 matrix of per-message-type × per-concern modules
  (each 1–15 classes, fan-in 2–5) plus **three meta modules that contain no source and are referenced by nothing**
  (fan-in 0). The matrix is only ever assembled as a whole to build the DSP stack.

Collapsing these clusters reduces the module count considerably without losing any meaningful separation: the libraries
still form an acyclic graph layered above the SPI modules.

## Approach

### Target modules and mapping

**Merged libraries** (left: the merge target, right: the modules folded into it):

| Target                  | Absorbs                                                                                                                                                                                                                                                                                                                                                             |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`dsp-lib`**           | all 13 `data-protocols/dsp/dsp-lib/*` modules — `dsp-catalog-{http-api,transform,validation}-lib`, `dsp-negotiation-{http-api,transform,validation}-lib`, `dsp-transfer-process-{http-api,transform,validation}-lib`, `dsp-version-transform-lib`, and the three empty meta modules (`dsp-catalog-lib`, `dsp-negotiation-lib`, `dsp-transfer-process-lib`, deleted) |
| **`jsonld-lib`**        | `json-lib`, `json-ld-lib`, `transform-lib`, `validator-lib`                                                                                                                                                                                                                                                                                                         |
| **`core-lib`**          | `query-lib`, `crypto-common-lib`, `keys-lib`, `token-lib`, `encryption-lib`, `auth-authentication-oauth2-lib`, `auth-authorization-oauth2-lib`, `verifiable-credentials-lib`, `decentralized-claims-lib`, `decentralized-claims-sts-remote-lib`,`jersey-providers-lib`, `boot-lib`, `http-lib`, `nats-lib`, `util-lib`, `sql-lib`, `api-lib`, `management-api-lib`  |
| **`control-plane-lib`** | `policy-engine-lib`, `policy-evaluator-lib`, `store-lib`, `control-plane-policies-lib`, `catalog-util-lib`, `control-plane-transfer-provision-lib`, `state-machine-lib`                                                                                                                                                                                             |

**Left standalone**: `jws2020-lib` because it is constrained by a dependency onto the `":extensions:common:json-ld"`).
This is a known issue and should be addressed in a future iteration, at which point the `jws2020-lib` can be folded into
`core-lib`.

