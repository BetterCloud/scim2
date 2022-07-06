#!/usr/bin/env groovy

/**
 * Build pipeline for the IAM service.
 */

def slackChannel = 'tpsvc-notifications'
def slackMonitoringDev = (env.BRANCH_NAME == 'develop' || env.BRANCH_NAME == 'master') ? 'tpsvc-monitoring-dev' : 'tpsvc-notifications'
def decodedJobName = env.JOB_NAME.replaceAll("%2F", "/")
def releaseBranchPrefix = 'do-release-'

/**
 * Credentials
 */
def nexusCredentials = usernamePassword(
    credentialsId: 'nexus-artifact-zl-credentials',
    passwordVariable: 'NEXUS_TALEND_PASSWORD',
    usernameVariable: 'NEXUS_TALEND_USER')

/**
 * Archive Artifacts
 */
def archiveBuildArtifacts() {
  archiveArtifacts artifacts: 'scim2-sdk-common/build/reports/', fingerprint: true, onlyIfSuccessful: true, allowEmptyArchive: true
  archiveArtifacts artifacts: 'spring-boot-starter-scim2/build/reports/', fingerprint: true, onlyIfSuccessful: true, allowEmptyArchive: true
}

/**
 * Pod configuration
 */
def podLabel = "scim2-${UUID.randomUUID().toString()}".take(53)

pipeline {

  agent {
    kubernetes {
      label podLabel
      yamlFile 'builderPodTemplate.yaml'
    }
  }

  parameters {
    booleanParam(name: 'PUBLISH_SNAPSHOT', defaultValue: false,
            description: '''
                 Do you want to publish a snapshot ?
               ''')
  }

  environment {
    NEXUS_TPSVC_URL                       = 'https://artifacts-zl.talend.com/nexus/content/repositories/tpsvc'
    NEXUS_TALEND_SNAPSHOTS_URL            = 'https://artifacts-zl.talend.com/nexus/content/repositories/snapshots'
    NEXUS_TALEND_RELEASES_URL             = 'https://artifacts-zl.talend.com/nexus/content/repositories/releases'
    NEXUS_TALEND_OS_RELEASE_URL           = 'https://artifacts-zl.talend.com/nexus/content/repositories/TalendOpenSourceRelease'
    NEXUS_TALEND_OPEN_SOURCE_RELEASE_URL  = 'https://artifacts-zl.talend.com/nexus/content/repositories/TalendOpenSourceRelease'
    NEXUS_TALEND_OPEN_SOURCE_SNAPSHOT_URL = 'https://artifacts-zl.talend.com/nexus/content/repositories/TalendOpenSourceSnapshot'

    GIT_USER  = 'build-talend-tpsvc'
    GIT_EMAIL = 'build-talend-tpsvc@talend.com'
    GIT_CREDS = credentials('github-credentials')

    BUILD_PROFILE = 'prod'
    BUILD_ENV = 'ci'
    BUILD_OPTS = '--stacktrace'
    GRADLE_OPTS = '-Dorg.gradle.daemon=false -Dorg.gradle.workers.max=1 -Xmx1500m'
    SERVICE_REPO_PATH = "Talend/scim2"
  }

  options {
    buildDiscarder(logRotator(artifactNumToKeepStr: '5', numToKeepStr: env.BRANCH_NAME == 'develop' ? '10' : '2'))
    timeout(time: 60, unit: 'MINUTES')
    skipStagesAfterUnstable()
    disableConcurrentBuilds()
  }

  stages {
    stage('Announce Build Start') {
      steps {
        slackSend (color: '#0000AA', channel: "${slackChannel}", message: "STARTED: `${decodedJobName}` #${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)")
      }
    }

    stage('Set Gradle options') {
      steps {
        container('talend-tsbi-springboot-builder') {
          withCredentials([nexusCredentials]) {
            script {
              // inject required credentials (then we don't need the gradle settings file)
              env.ORG_GRADLE_PROJECT_nexusTpsvcUsername = "${NEXUS_TALEND_USER}"
              env.ORG_GRADLE_PROJECT_nexusTpsvcPassword = "${NEXUS_TALEND_PASSWORD}"
              env.ORG_GRADLE_PROJECT_nexusTpsvcUrl      = env.NEXUS_TPSVC_URL

              env.ORG_GRADLE_PROJECT_nexusTalendSnapshotsUsername = "${NEXUS_TALEND_USER}"
              env.ORG_GRADLE_PROJECT_nexusTalendSnapshotsPassword = "${NEXUS_TALEND_PASSWORD}"
              env.ORG_GRADLE_PROJECT_nexusTalendSnapshotsUrl      = env.NEXUS_TALEND_SNAPSHOTS_URL

              env.ORG_GRADLE_PROJECT_nexusTalendReleasesUsername = "${NEXUS_TALEND_USER}"
              env.ORG_GRADLE_PROJECT_nexusTalendReleasesPassword = "${NEXUS_TALEND_PASSWORD}"
              env.ORG_GRADLE_PROJECT_nexusTalendReleasesUrl      = env.NEXUS_TALEND_RELEASES_URL

              env.ORG_GRADLE_PROJECT_nexusTalendOpenSourceReleaseUsername = "${NEXUS_TALEND_USER}"
              env.ORG_GRADLE_PROJECT_nexusTalendOpenSourceReleasePassword = "${NEXUS_TALEND_PASSWORD}"
              env.ORG_GRADLE_PROJECT_nexusTalendOSReleasesUrl             = env.NEXUS_TALEND_OS_RELEASE_URL
              env.ORG_GRADLE_PROJECT_nexusTalendOpenSourceReleaseUrl      = env.NEXUS_TALEND_OPEN_SOURCE_RELEASE_URL
              env.ORG_GRADLE_PROJECT_nexusTalendOpenSourceSnapshotUrl     = env.NEXUS_TALEND_OPEN_SOURCE_SNAPSHOT_URL

              // envs required to publish artifacts
              env.ORG_GRADLE_PROJECT_nexusTpsvcDeployUsername = "${NEXUS_TALEND_USER}"
              env.ORG_GRADLE_PROJECT_nexusTpsvcDeployPassword = "${NEXUS_TALEND_PASSWORD}"
            }
          }
        }
      }
    }

    stage("Pre-Build") {
      when {
        anyOf {
          branch "${releaseBranchPrefix}patch"
          branch "${releaseBranchPrefix}minor"
          branch "${releaseBranchPrefix}major"
        }
      }
      steps {
        container('talend-tsbi-springboot-builder') {
          script {
            env.RECKON_SCOPE = env.BRANCH_NAME.toLowerCase().substring(releaseBranchPrefix.length())
            echo "Scope of the release is: ${RECKON_SCOPE}"
            BUILD_OPTS = "-Preckon.stage=final -Preckon.scope=${RECKON_SCOPE} ${BUILD_OPTS}"
          }
          sh '''
             git config --global --add safe.directory \$(pwd)
             set +x
             echo https://$GIT_CREDS_USR:$GIT_CREDS_PSW@github.com > /tmp/.git-credentials
             git checkout $BRANCH_NAME
             git config credential.helper 'store --file /tmp/.git-credentials\'
             git config user.name "$GIT_USER"
             git config user.email "$GIT_EMAIL"
             git clean -fdxn
             git fetch --tags
             git status
          '''
        }
      }
    }

    stage('Build') {
      environment {
        TAG="${BRANCH_NAME}"
      }
      steps {
        container('talend-tsbi-springboot-builder') {
          sh "gradle ${BUILD_OPTS} clean build"
        }
      }
      post {
        always {
          archiveBuildArtifacts()
        }
      }
    }

    stage('Publish snapshot') {
      when {
        not {
          anyOf {
            branch "${releaseBranchPrefix}patch"
            branch "${releaseBranchPrefix}minor"
            branch "${releaseBranchPrefix}major"
          }
        }
        expression { return params.PUBLISH_SNAPSHOT }
      }
      steps {
        container('talend-tsbi-springboot-builder') {
          sh "gradle ${BUILD_OPTS} publish"
        }
      }
    }

    stage('Release') {
      when {
        anyOf {
          branch "${releaseBranchPrefix}patch"
          branch "${releaseBranchPrefix}minor"
          branch "${releaseBranchPrefix}major"
        }
      }
      environment {
        GRGIT_USER="$GIT_CREDS_USR"
        GRGIT_PASS="$GIT_CREDS_PSW"
      }
      steps {
        container('talend-tsbi-springboot-builder') {
          sh "gradle ${BUILD_OPTS} publish reckonTagPush"
          sh "git push --delete origin ${BRANCH_NAME}"
        }
      }
    }
  }

  post {
    success {
      slackSend (color: 'good', channel: "${slackChannel}", message: "SUCCESSFUL: `${decodedJobName}` #${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)\nDuration: ${currentBuild.durationString}")
    }
    unstable {
      slackSend (color: 'warning', channel: "${slackChannel}", message: "UNSTABLE: `${decodedJobName}` #${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)")
    }
    failure {
      slackSend (color: '#AA0000', channel: "${slackMonitoringDev}", message: "FAILED: `${decodedJobName}` #${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)")
    }
    aborted {
      slackSend (color: 'warning', channel: "${slackChannel}", message: "ABORTED: `${decodedJobName}` #${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)")
    }
  }
}
