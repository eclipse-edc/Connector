#!/usr/bin/env bash

curl  -H "Authorization: Basic $AZURE_PAT" \
"https://feeds.dev.azure.com/sovity/41799556-91c8-4df6-8ddb-4471d6f15953/_apis/packaging/Feeds/core-edc/packages?protocolType=maven&includeUrls=false&includeAllVersions=true&includeDeleted=false&api-version=7.0" \
| jq --raw-output '.value[].name' | cut -d ':' -f 2 | while read -r arti
do
  curl \
    -X PATCH \
    -H "Authorization: Basic $AZURE_PAT" \
    "https://pkgs.dev.azure.com/sovity/41799556-91c8-4df6-8ddb-4471d6f15953/_apis/packaging/feeds/core-edc/maven/groups/org.eclipse.edc/artifacts/${arti}/versions/${VERSION}?api-version=7.1-preview.1" \
    --json @- <<"EOF"
{
  "views": {
    "op": "add",
    "path": "/views/-",
    "value": "Release"
  }
}

EOF

  if [[ $? -ne 0 ]]
  then
    echo "Failed to promote $arti"
    exit 1
  fi

done
