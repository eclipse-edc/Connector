#!/usr/bin/env bash

# Core EDC https://dev.azure.com/sovity/Core%20EDC
PROJECT="41799556-91c8-4df6-8ddb-4471d6f15953"
#core-edc feed https://dev.azure.com/sovity/Core%20EDC/_artifacts/feed/core-edc
FEED="core-edc"

curl -u ":$AZURE_PAT" \
"https://feeds.dev.azure.com/sovity/$PROJECT/_apis/packaging/Feeds/$FEED/packages?protocolType=maven&includeUrls=false&includeAllVersions=true&includeDeleted=false&api-version=7.0" \
| jq --raw-output '.value[].name' | cut -d ':' -f 2 | while read -r arti
do
  echo "Promoting $arti ..."
  curl \
    -X PATCH \
    -u ":$AZURE_PAT" \
    "https://pkgs.dev.azure.com/sovity/$PROJECT/_apis/packaging/feeds/core-edc/maven/groups/org.eclipse.edc/artifacts/${arti}/versions/${VERSION}?api-version=7.1-preview.1" \
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
  else
    echo "Promoted $arti"
  fi

done
