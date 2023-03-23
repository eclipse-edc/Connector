#!/bin/bash

set -euo pipefail

output=runtime_settings.properties

terraform output -json | jq -r 'keys[] as $k | "\($k | ascii_downcase | gsub("_";"."))=\(.[$k] | .value)"' > ../$output

echo Created $output

