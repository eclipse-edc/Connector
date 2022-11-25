#!/bin/bash
#   Copyright (c) 2022 Microsoft Corporation
#
#   This program and the accompanying materials are made available under the
#   terms of the Apache License, Version 2.0 which is available at
#   https://www.apache.org/licenses/LICENSE-2.0
#
#   SPDX-License-Identifier: Apache-2.0
#
#   Contributors:
#        Microsoft Corporation - initial implementation

# This file is intended to be used as Post-Build action on CI pipelines to post a message to the EDC's #jenkins-ci Discord
# channel.
# It is loosely based on https://github.com/symboxtra/universal-ci-discord-webhook/blob/master/send.sh, but with many simplifications.

# todo: make configurable
BRANCH_NAME="main"

WEBHOOK_URL="$1"
STATUS="$2"
JENKINS_JOB="$3"
BUILD_NUMBER="$4"
REPO_URL="$5"
CONTENT="$6"

# do not run script if required parameters are not supplied
if [ "$#" -lt 5 ]; then
  echo "usage: discord_webhook.sh WEBHOOK_URL STATUS JOB_NAME BUILD_NUMBER REPO_URL CONTENT"
  echo " WEBHOOK_URL   = URL of the webhook to invoke, e.g. for discord"
  echo " STATUS        = \"success\" or \"failure\". Will use \"Unknown\" when anything else is passed"
  echo " JOB_NAME      = name of the job EXACTLY as configured in Jenkins. Use quotes if the job name contains blanks"
  echo " BUILD_NUMBER  = jenkins build number, must be an integer"
  echo " REPO_URL      = URL to the (Github) repository"
  echo " CONTENT       = [OPTIONAL] a string containing message content to be posted to. Defaults to \"I finished a job\""
  exit 1
fi

if [ -z "$6" ]; then
  echo "No content supplied, using default."
  CONTENT="I finished a job"
fi

echo "'"WEBHOOK_URL:  "${WEBHOOK_URL}""'"
echo "'"STATUS:       "${STATUS}""'"
echo "'"JENKINS_JOB:  "${JENKINS_JOB}""'"
echo "'"BUILD_NUMBER: "${BUILD_NUMBER}""'"
echo "'"REPO_URL:     "${REPO_URL}""'"
echo "'"CONTENT:      "${CONTENT}""'"


CI_PROVIDER="Jenkins"
DISCORD_AVATAR="https://wiki.jenkins.io/download/attachments/2916393/headshot.png?version=1&modificationDate=1302753947000&api=v2"
SUCCESS_AVATAR="https://jenkins.io/images/logos/cute/cute.png"
FAILURE_AVATAR="https://jenkins.io/images/logos/fire/fire.png"
UNKNOWN_AVATAR="https://www.jenkins.io/images/logos/mono/mono.png"

JOB_URL="https://ci.eclipse.org/dataspaceconnector/job/${JENKINS_JOB}"
BUILD_URL="${JOB_URL}/${BUILD_NUMBER}"
BUILD_URL="${BUILD_URL}/console"

echo
echo -e "[Webhook]: ${CI_PROVIDER} CI detected."
echo -e "[Webhook]: Sending webhook to Discord..."
echo

case ${STATUS} in
"success")
  EMBED_COLOR=3066993
  STATUS_MESSAGE="Passed"
  AVATAR="${SUCCESS_AVATAR}"
  ;;
"failure")
  EMBED_COLOR=15158332
  STATUS_MESSAGE="Failed"
  AVATAR="${FAILURE_AVATAR}"
  ;;
*)
  EMBED_COLOR=8421504
  STATUS_MESSAGE="Status Unknown"
  echo "status \"${STATUS}\" --> ${STATUS_MESSAGE}"
  AVATAR="${UNKNOWN_AVATAR}"
  ;;
esac

TIMESTAMP=$(date -u +%FT%TZ)
WEBHOOK_DATA='{
  "username": "Jenkins CI",
  "content": "'"${CONTENT}"'",
  "avatar_url": "'"${DISCORD_AVATAR}"'",
  "embeds": [ {
    "color": '${EMBED_COLOR}',
    "author": {
      "name": "'"${CI_PROVIDER}"' '"${JENKINS_JOB}  #${BUILD_NUMBER}"' - '"${STATUS_MESSAGE}"'",
      "url": "'"${BUILD_URL}"'",
      "icon_url": "'"${AVATAR}"'"
    },
    "title": "'"${JENKINS_JOB} - ${STATUS_MESSAGE}"'",
    "url": "'"${JOB_URL}"'",
    "fields": [
      {
        "name": "Job Name",
        "value": "'"[${JENKINS_JOB%}](${JOB_URL})"'",
        "inline": true
      },
      {
        "name": "Build Number",
        "value": "'"[${BUILD_NUMBER%.*}](${BUILD_URL})"'",
        "inline": true
      },
      {
        "name": "Branch/Tag",
        "value": "'"[\`${BRANCH_NAME}\`](${REPO_URL}/tree/${BRANCH_NAME})"'",
        "inline": true
      }
    ],
    "timestamp": "'"${TIMESTAMP}"'"
  } ]
}'

curl --fail --progress-bar -A "${CI_PROVIDER}-Webhook" -H "Content-Type:application/json" -d "${WEBHOOK_DATA}" "${WEBHOOK_URL}"

if [ $? -ne 0 ]; then
  echo -e "Webhook data:\\n${WEBHOOK_DATA}"
  echo -e "\\n[Webhook]: Unable to send webhook."

  # Exit with an error signal
  exit 1
else
  echo -e "\\n[Webhook]: Successfully sent the webhook."
fi
