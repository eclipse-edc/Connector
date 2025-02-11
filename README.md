<!-- Improved compatibility of back to top link: See: https://github.com/othneildrew/Best-README-Template/pull/73 -->

<a name="readme-top"></a>

<!-- PROJECT LOGO -->
<br />
<div align="center">
<a href="https://github.com/sovity/core-edc">
<img src="https://raw.githubusercontent.com/sovity/edc-ui/main/src/assets/images/sovity_logo.svg" alt="Logo" width="300">
</a>

<h3 align="center">EDC Connector</h3>
<p align="center" style="padding-bottom:16px">
Fork for supporting older Eclipse EDC versions
</p>
</div>

## About The Project

This is a fork of the [Eclipse EDC Connector](https://github.com/eclipse-edc/Connector) with additions by [sovity](https://sovity.de/).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Branches

- The `default` branch serves as a default branch for this fork, only containing an entry point with the main README and CI.
- The `main` branch tracks the **Eclipse EDC** `main` branch. It is not used as the main branch in this fork.
- The `sovity/x.y.z` branches will contain forked **Eclipse EDC** releases with sovity commits on top.
    - [sovity/0.2.1](https://github.com/sovity/core-edc/blob/sovity/0.2.1/CHANGELOG.md)
    - [sovity/0.7.2](https://github.com/sovity/core-edc/blob/sovity/0.7.2/CHANGELOG.md)

Each branch has its own Changelog, as patches are always applied on top of releases and the entire history of the Eclipse EDC Connector is not maintained.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Usage

### Support a new version

Adding a new version can be done by following the [new supported version procedure](https://github.com/sovity/core-edc/issues/new?template=new-supported-version.md).

### Patching a supported version

Works like a regular repository but the main branches are `sovity/` and must be selected explicitly.

Adding a new patch on a supported version can be done by following the [new patch procedure](https://github.com/sovity/core-edc/issues/new?template=new-patch.md).

### Releasing a supported version

Releasing a supported version can be done by following the [releasing procedure](https://github.com/sovity/core-edc/issues/new?template=release.md).

### Deliveries

The artifacts for this repository are *not* available via the GitHub Maven Repository **but via our own Maven Registry on Azure**. This is because the many artifacts created in this repository caused the GitHub Maven Repository to be a bottleneck for the CI. We also needed better mirroring of transitive dependencies, as transitive dependencies of the Eclipse EDC Connector have disappeared before.

The artifacts are released under the same group id as the original EDC: `org.eclipse.edc` and the same artifact names. Only the version differs and usually matches the pattern `a.b.c.d` with `a.b.c` matching the Eclipse EDC version it was based on.

#### Production

The sovity Maven Registry on Azure is public and can be added to any Maven or Gradle Project [using this URL](https://pkgs.dev.azure.com/sovity/41799556-91c8-4df6-8ddb-4471d6f15953/_packaging/core-edc/maven/v1).

#### Test

The artifacts on the `sovity/*` branches, before releasing, are published in the [test feed](https://dev.azure.com/sovity/Test/_artifacts/feed/test.


<p align="right">(<a href="#readme-top">back to top</a>)</p>


## Contributing

To submit changes to the Eclipse EDC, please refer to [the original repository](https://github.com/eclipse-edc/Connector)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Releasing

[Create a Release Issue](https://github.com/sovity/core-edc/issues/new?assignees=&labels=task%2Frelease%2Cscope%2Fcore&projects=&template=release.md&title=Release+X.Y.Z.W) and follow the instructions.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## License

Distributed under the Apache 2.0 License. See `LICENSE` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Contact

contact@sovity.de

<p align="right">(<a href="#readme-top">back to top</a>)</p>

