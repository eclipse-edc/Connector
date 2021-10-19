#!/bin/bash

# Usage:
#   generateThirdPartyReport.sh /path/inputFilename
#
# Prerequisites:
# The Eclipse Dash Licenses tool was executed and resulted in an file (e.g., NOTICES)
# listing all dependencies of the project.
# To process the dependencies of a specific module, execute the following command:
#   gradle dependencies | grep -Poh "(?<=\s)[\w\.-]+:[\w\.-]+:[^:\s]+" | sort | uniq | java -jar /path/org.eclipse.dash.licenses-0.0.1-SNAPSHOT.jar - -summary NOTICES
#
# Comments:
# Gradle doesn't support listing all dependencies of a multi-module project out of the box.
# The following custom task can be used to achieve this:
#   subprojects {
#     tasks.register<DependencyReportTask>("allDependencies"){}
#   }
#

input="$1"
output="./Third-Party-Content.md"

touch "$output"
echo -n "" > "$output"
echo "## Third Party Content" >> "$output"

while IFS= read -r line
do
  #Results are separated by comma
  IFS=', ' read -r -a row <<< "$line"
  dependency="${row[0]}"

  #Dependencies which could not be resolved are marked as invalid
  if  [[ $dependency == maven/mavencentral/* ]];
  then
      length=${#dependency}
      mavenPath=${dependency:19:length}

      IFS='/ ' read -r -a hierarchy <<< "$mavenPath"

      groupId=${hierarchy[0]}
      #Periods must be replaced with slashes to be used later in the context of an URL
      groupIdUrl=${groupId//./$'/'}
      artifactId=${hierarchy[1]}
      version=${hierarchy[2]}

      path="$groupIdUrl/$artifactId/$version/$artifactId-$version.pom"

      #Example URL: https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.12.1/jackson-core-2.12.1.pom
      targetUrl="https://repo1.maven.org/maven2/$path"

      pomFile=$(curl $targetUrl)

      projectName=$(xmllint --xpath "/*[local-name() = 'project']/*[local-name() = 'name']/text()" - <<< $pomFile)
      projectUrl=$(xmllint --xpath "/*[local-name() = 'project']/*[local-name() = 'url']/text()" - <<< $pomFile)
      scmUrl=$(xmllint --xpath "/*[local-name() = 'project']/*[local-name() = 'scm']/*[local-name() = 'connection']/text()" - <<< $pomFile)

      if  [[ ! -z "$projectName" ]];
        then
          echo "$projectName" >> "$output"

          if  [[ ! -z "$projectUrl" ]];
            then
              echo "* Project: $projectUrl" >> "$output"
          fi

          if  [[ ! -z "$scmUrl" ]];
            then
                echo "* Source: $scmUrl" >> "$output"
          fi

          echo "* Maven Artifact: $mavenPath" >> "$output"
          echo "* License: ${row[1]}" >> "$output"
          echo "" >> "$output"
      fi
  fi

done < "$input"