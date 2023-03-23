#!/bin/bash

set -euxo pipefail

cd $(dirname "$0")

. util/terraform-init.sh
. ../util/terraform-download-output.sh
