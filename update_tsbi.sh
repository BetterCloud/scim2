#!/usr/bin/env bash

set -x

#################################################################################################################################
## This script first finds the latest TSBI version, then compares it to the current version. If they differ,
## it updates TSBI and Spring-Boot versions to the provided by latest TSBI.
#################################################################################################################################

netrcFile="$1"
nexusReleasesUrl="$2"
artifactoryUrl="$3"
nexusUrl="$4"

if [ -z "$netrcFile" ]; then
  echo '.netrc file path has to be the first argument.'
  echo 'The file should contain two lines like this:'
  echo 'machine artifacts-zl.talend.com login <nexus-username> password <nexus-password>'
  echo 'machine artifactory.datapwn.com login <artifactory-username> password <artifactory-password>'
  exit 2
fi

if [ -z "$nexusReleasesUrl" ]; then
  nexusReleasesUrl="https://artifacts-zl.talend.com/nexus/content/repositories/releases"
fi

if [ -z "$artifactoryUrl" ]; then
  artifactoryUrl="https://artifactory.datapwn.com/artifactory"
fi

if [ -z "$nexusUrl" ]; then
  nexusUrl="https://artifacts-zl.talend.com/nexus/content/repositories/tpsvc"
fi

# exit status 0 means the script has updated the version
#             1 means the script ran successfully but the project already uses the latest dependency versions
#             2 or higher means the script finished with errors
exitStatus=1


#################################################################################################################################

# Params
#   $1 - Nexus Repo
#   $2 - Artifact path in Nexus
function getLatestVersionFromNexus() {
  curl -L -s --netrc-file "$netrcFile" "$1/$2/maven-metadata.xml" | grep -F '<release>' | grep -o '[0-9.-]*'
}

# Params
#   $1 - POM
#   $2 - Version param name (for example, "spring-boot.version")
function getVersionFromPom() {
  echo "$1" | grep -F "<$2>" | sed "s#.*<$2>##g" | sed "s#</$2>.*##g"
}

# Params
#   $1 - Property name
function getVersionFromGradleProperties() {
  grep -Fi "$1" gradle.properties | sed "s/$1.*=//g"
}

# Params
#   $1 - Version param name in gradle.properties (for example, "tsbiBomVersion")
#   $2 - New version
function updateGradleProperties() {
  sed -i "s/$1=.*/$1=$2/g" gradle.properties
}

# Params
#   $1 - Dependency name (for example "Platform SDK")
#   $2 - Property name (for example "platformSdkVersion")
#   $3 - Nexus repo for the dependency (for example "$nexusUrl")
#   $4 - Dependency path in Nexus repo (for example, "org/talend/platform/sdk/scim-client")
function updateDependencyIfNecessary() {
  dependencyName="$1"
  propertyName="$2"
  nexusRepo="$3"
  nexusDependencyPath="$4"

  currentDependencyVersion=$(getVersionFromGradleProperties "$propertyName")

  if [ -n "$currentDependencyVersion" ] ; then
    latestDependencyVersion=$(getLatestVersionFromNexus "$nexusRepo" "$nexusDependencyPath")
    if [ -z "$latestDependencyVersion" ] ; then
      echo "Unable to get latest $dependencyName version"
      exit 2
    fi

    echo "Current $dependencyName: $currentDependencyVersion, Latest: $latestDependencyVersion"

    if [ "$currentDependencyVersion" != "$latestDependencyVersion" ] ; then
      echo "Updating $dependencyName ..."

      updateGradleProperties "$propertyName" "$latestDependencyVersion"

      echo "Done updating $dependencyName"
      return 0
    fi
  else
    echo "This project doesn't use $dependencyName"
  fi

  return 1
}

#################################################################################################################################
### Updating TSBI and Spring-Boot

function getFullCurrentTsbiBomVersion() {
  getVersionFromGradleProperties 'tsbiBomVersion'
}

function getSpringBootKind() {
  getFullCurrentTsbiBomVersion | sed 's/:.*//g'
}

function getCurrentTsbiBomVersion() {
  getFullCurrentTsbiBomVersion | sed 's/.*://g'
}

# Params
#   $1 - Nexus Repo
#   $2 - Spring Boot kind (for example, 2.3)
function getLatestTsbiBomVersion() {
  getLatestVersionFromNexus "$1" "org/talend/tsbi/java/springboot-bom/$2"
}

# Params
#   $1 - Nexus Repo
#   $2 - Spring Boot kind (for example, 2.3)
#   $3 - TSBI POM version
function getTsbiBom() {
  curl -L -s --netrc-file "$netrcFile" "$1/org/talend/tsbi/java/springboot-bom/$2/$3/$2-$3.pom"
}

#################################################################################################################################

currentTsbiSbKind=$(getSpringBootKind)
currentTsbiBomVersion=$(getCurrentTsbiBomVersion)

if [ -z "$currentTsbiBomVersion" ]; then
  echo "This project doesn't seem to use TSBI BOM"
  exit 1
fi

latestTsbiVersion=$(getLatestTsbiBomVersion "$nexusReleasesUrl" "$currentTsbiSbKind")

if [ -z "$latestTsbiVersion" ]; then
  echo "Unable to get latest TSBI BOM version"
  exit 2
fi

echo "Current TSBI BOM: $currentTsbiBomVersion, Latest: $latestTsbiVersion"

if [ "$currentTsbiBomVersion" != "$latestTsbiVersion" ]; then
  echo "Getting latest TSBI BOM ..."

  latestTsbiBom=$(getTsbiBom "$nexusReleasesUrl" "$currentTsbiSbKind" "$latestTsbiVersion")

  if [ -z "$latestTsbiBom" ]; then
    echo "Unable to get TSBI BOM"
    exit 2
  fi

  latestSpringBootVersion=$(getVersionFromPom "$latestTsbiBom" "spring-boot.version")

  if [ -z "$latestSpringBootVersion" ]; then
    echo "Unable to get Spring-Boot version from TSBI BOM"
    exit 2
  fi

  echo "Updating to TSBI BOM $latestTsbiVersion and Spring-Boot $latestSpringBootVersion"

  updateGradleProperties "tsbiBomVersion" "$currentTsbiSbKind:$latestTsbiVersion"
  updateGradleProperties "springBootVersion" "$latestSpringBootVersion"

  exitStatus=0
  echo "Done updating TSBI BOM and Spring-Boot"
fi

#################################################################################################################################
### Updating misc dependencies

updateDependencyIfNecessary "Platform SDK" "platformSdkVersion" "$nexusUrl" "org/talend/platform/sdk/scim-client"
exitStatus=$((exitStatus & $?))

updateDependencyIfNecessary "Security BOM" "securityBomVersion" "$nexusUrl" "org/talend/pscommons/security-bom"
exitStatus=$((exitStatus & $?))

#################################################################################################################################
### Updating TSBI image

function getCurrentTsbiImageVersion() {
  grep -Fe artifactory.datapwn.com builderPodTemplate.yaml | grep -F tsbi | head -1 | sed 's/.*://g' | sed 's/-.*-/-/g'
}

# Params
#   $1 - TSBI image path in artifactory
function getLatestTsbiImageVersion() {
  artifactory_url="${1/artifactory.datapwn.com/${artifactoryUrl}}"
  curl -L -s --netrc-file "$netrcFile" "$artifactory_url" | grep -o "[0-9][0-9.-]*-[0-9][0-9]*" | sort --version-sort | tail -1
}

# Params
#   $1 - TSBI image path in artifactory
#   $2 - TSBI image version
function getTsbiImageDigest() {
  artifactory_url=$(echo "$1" | sed "s@artifactory.datapwn.com@${artifactoryUrl}/api/docker@g")
  artifactory_url=$(echo "$artifactory_url" | sed 's@tlnd-docker-\([a-zA-Z0-9]\+\)@tlnd-docker-\1/v2@g')
  headers=$(curl -I -L -s --netrc-file "$netrcFile" \
      -H "Accept: application/vnd.docker.distribution.manifest.v2+json" \
      "$artifactory_url/manifests/$2" | grep -Fi 'Docker-Content-Digest' | tr -d '\r')
  echo -n "${headers}" | grep -o 'sha.*'
}

function getBaseImageName() {
  base_image=$(grep -Fe dockerBaseImage gradle.properties | grep -o 'artifactory.datapwn.com.*@sha' | sed 's/@sha//g')
  java_version=$(grep -e 'javaVersion[ ]*=' gradle.properties | grep -o '[0-9][0-9]*')
  echo "${base_image//\$\{javaVersion\}/${java_version}}"
}

function getBaseImageDigest() {
  grep -Fe dockerBaseImage gradle.properties | grep -o '@sha.*' | sed 's/@//g'
}

# Params
#   $1 - Current TSBI Image version
#   $2 - Latest TSBI Image version
#   $3 - File name for update
function updateTsbiImage() {
  sed -i "s/$1/$2/g" "$3"
}

#################################################################################################################################

currentTsbiImageVersion=$(getCurrentTsbiImageVersion)

if [ -z "$currentTsbiImageVersion" ]; then
  echo "Unable to get current TSBI Image version"
fi

baseImageName=$(getBaseImageName)

echo "Base image name is $baseImageName"

latestTsbiImageVersion=$(getLatestTsbiImageVersion $baseImageName)

if [ -z "$latestTsbiImageVersion" ]; then
  echo "Unable to get latest TSBI Image version"
fi

echo "Current TSBI Image: $currentTsbiImageVersion, Latest: $latestTsbiImageVersion"

if [ "$currentTsbiImageVersion" != "$latestTsbiImageVersion" ]; then
  echo "Updating TSBI Image version"

  updateTsbiImage "$currentTsbiImageVersion" "$latestTsbiImageVersion" "builderPodTemplate.yaml"
  updateTsbiImage "$currentTsbiImageVersion" "$latestTsbiImageVersion" "Jenkinsfile"

  currentImageDigest=$(getBaseImageDigest)
  latestImageDigest=$(getTsbiImageDigest "$baseImageName" "$latestTsbiImageVersion")

  updateTsbiImage "$currentImageDigest" "$latestImageDigest" "gradle.properties"

  exitStatus=0
fi

#################################################################################################################################

echo "Done updating versions"

exit $exitStatus
