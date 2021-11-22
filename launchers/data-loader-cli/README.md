# CLI DataLoader tool

This is a command line tool that can be used to load data from a file (JSON, CSV,...)
and store it in a backing persistence system. As a first use case, we implemented loading
`Asset` and `DataAddress` objects (or actually `AssetEntry`, which is wrapper for those two) objects from a JSON file
and storing them using an `AssetLoader`.

For an example JSON file take a look at [this file](src/test/resources/assets.json).

## Synopsis:

```bash
java -jar <path-to-jar> --assets <path-to-file.json>
```

## A few things to notice

- by default no `AssetIndex` implementation is configured! Please adapt the `build.gradle.kts` file to suit your
  particular needs! The app _will_ log an error if you don't do this.
- If a database-backed AssetIndex is used, you'll likely also need a configuration extension plus a vault extension to
  handle credentials.
- The commandline tool is intended to run as standalone program
- Currently only Asset/DataEntry objects are supported, more commands will follow. This might change the synopsis of the
  tool.