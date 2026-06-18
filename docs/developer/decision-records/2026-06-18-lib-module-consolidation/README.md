# Lib Module Consolidation

## Decision

As a companion to the SPI consolidation (see `2026-06-18-spi-module-consolidation`), we will consolidate the ~43
`*-lib` modules by collapsing over-granular clusters and tiny 1:1 dependency chains into a smaller set of
domain-cohesive libraries.

Unlike the SPI modules, the `lib` modules are _implementation_ libraries — reusable code that registers no extensions
— so this is not an audience-based re-tiering. It is the removal of module boundaries that no longer earn their keep.
The notable merges are:

- **`dsp-lib`** — collapse the 13 modules under `data-protocols/dsp/dsp-lib` (the `{catalog, negotiation,
  transfer-process} × {http-api, transform, validation}` matrix, plus `dsp-version-transform-lib`, plus three empty
  umbrella modules) into a single library.
- **`transform-lib`** — the JSON / JSON-LD / transform / validation serialization stack.
- **`token-lib`** — the crypto / keys / token / encryption stack.
- **`store-lib`**, **`policy-engine-lib`**, **`decentralized-claims-lib`**, **`auth-oauth2-lib`** and
  **`control-plane-lib`** — domain-cohesive merges of small sibling libraries.

We accept that this removes published Maven coordinates and is therefore a breaking change for downstream adopters that
depend on the individual modules directly.

## Rationale

The same forces that motivated the SPI consolidation apply to the `lib` tree, which has grown to ~43 modules:

- Many libraries are tiny (several contain a single class or a handful), split along historical lines, and are only
  ever consumed together.
- The split adds build, wiring and publishing overhead for no separation-of-concerns benefit — publishing every module
  individually to Maven Central is a meaningful part of the ~2:45h publish time.
- The `data-protocols/dsp/dsp-lib` tree is the clearest case: a 3×3 matrix of per-message-type × per-concern modules
  (each 1–15 classes, fan-in 2–5) plus **three umbrella modules that contain no source and are referenced by nothing**
  (fan-in 0). The matrix is only ever assembled as a whole to build the DSP stack.

Collapsing these clusters reduces the module count by roughly a quarter (~43 → ~18) without losing any meaningful
separation: the libraries still form an acyclic graph layered above the SPI modules.

## Approach

### Target modules and mapping

**Merged libraries** (left: the merge target, right: the modules folded into it):

| Target | Absorbs |
|---|---|
| **`dsp-lib`** | all 13 `data-protocols/dsp/dsp-lib/*` modules — `dsp-catalog-{http-api,transform,validation}-lib`, `dsp-negotiation-{http-api,transform,validation}-lib`, `dsp-transfer-process-{http-api,transform,validation}-lib`, `dsp-version-transform-lib`, and the three empty umbrellas (`dsp-catalog-lib`, `dsp-negotiation-lib`, `dsp-transfer-process-lib`, deleted) |
| **`transform-lib`** | `json-lib`, `json-ld-lib`, `transform-lib`, `validator-lib` |
| **`token-lib`** | `crypto-common-lib`, `keys-lib`, `token-lib`, `encryption-lib` |
| **`store-lib`** | `query-lib`, `store-lib` |
| **`policy-engine-lib`** | `policy-engine-lib`, `policy-evaluator-lib` |
| **`decentralized-claims-lib`** | `decentralized-claims-lib`, `verifiable-credentials-lib`, `decentralized-claims-sts-remote-lib` |
| **`auth-oauth2-lib`** | `auth-authentication-oauth2-lib`, `auth-authorization-oauth2-lib` |
| **`control-plane-lib`** | `control-plane-policies-lib`, `catalog-util-lib`, `control-plane-transfer-provision-lib` |

**Left standalone** (foundational, distinct, or — for `jws2020-lib` — layering-constrained):

```
util-lib  sql-lib  state-machine-lib  boot-lib  http-lib  nats-lib
api-lib  management-api-lib  jersey-providers-lib  jws2020-lib
```

### Layering

The `lib` modules sit above the SPI layer (they depend on SPIs and on each other) and form an acyclic graph. Each
merge folds a tight cluster or a 1:1 chain (e.g. `policy-evaluator-lib` has fan-in 1 and is used only by
`policy-engine-lib`; `token-lib` already depends on `crypto-common-lib`), so no new cycles are introduced. Cross-lib
dependencies that remain (e.g. `store-lib → util-lib`, `dsp-lib → transform-lib`) are one-directional.

### Open points

- **DSP collapse granularity.** The plan collapses the whole `dsp-lib` tree into a single `dsp-lib`. If a finer split
  is desired, the natural alternative is three libraries by concern — `dsp-transform-lib`, `dsp-validation-lib`,
  `dsp-http-api-lib` — rather than the current per-message-type × per-concern matrix.
- **`jws2020-lib` stays standalone.** It belongs to the crypto stack by domain, but it depends on an _extension_
  (`extensions:common:json-ld`), so folding it into a `core/common/lib` crypto library would invert the layering. It
  stays put unless that dependency is untangled first.
- **`api-lib` and `management-api-lib` stay separate.** Although 23 modules depend on both, they sit at different
  layers: `api-lib` is generic (depends only on `core-spi` + `validator-lib`) and is reused by non-management APIs,
  whereas `management-api-lib` pulls in `web-spi` and `contract-spi`. Merging would either inject a control-plane SPI
  into the generic library or bury generic helpers inside a management-specific module.
- **Edge cases.** As with the SPI consolidation, modules discovered during refactoring that don't clearly fit a target
  should default to remaining standalone rather than being forced into an unrelated library.
