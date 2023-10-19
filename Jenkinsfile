#!/usr/bin/env groovy

/**
 * Build pipeline for a TPSVC service.
 */

def slackChannel = 'tpsvc-notifications'
def slackMonitoringDev = (env.BRANCH_NAME == 'develop' || env.BRANCH_NAME == 'master') ? 'tpsvc-monitoring-dev' : 'tpsvc-notifications'
def decodedJobName = env.JOB_NAME.replaceAll("%2F", "/")
def releaseBranchPrefix = 'do-release-*'

def artifactoryCredentials = usernamePassword(
        credentialsId: 'artifactory-datapwn-credentials',
        passwordVariable: 'ARTIFACTORY_TALEND_PASSWORD',
        usernameVariable: 'ARTIFACTORY_TALEND_LOGIN')
def nexusCredentials = usernamePassword(
        credentialsId: 'nexus-artifact-zl-credentials',
        passwordVariable: 'NEXUS_TALEND_PASSWORD',
        usernameVariable: 'NEXUS_TALEND_USER')

def gradlew(String... args) {
    sh "./gradlew -s --max-workers=2 --no-daemon ${args.join(' ')}"
}

def hasChangesIn(String module) {
    // If there is no change target, we consider that the module has changed
    if (env.CHANGE_TARGET == null) {
        return true;
    }
    // Get last commit hash from origin/{TARGET}
    def TARGET = sh(
            returnStdout: true,
            script: "git rev-parse origin/${env.CHANGE_TARGET}"
    ).trim()
    // Get commit hash from HEAD commit
    def HEAD = sh(
            returnStdout: true,
            script: "git rev-parse HEAD"
    ).trim()
    // Get diff between CHANGE_SET and HEAD commits
    // Return true if a diff matches with module given as parameter
    return sh(
            returnStatus: true,
            script: "git diff --name-only ${TARGET}...${HEAD} | grep ^${module}/"
    ) == 0
}

static String getServiceName(String jobName) {
    // expected format: platform-services/<repo>/<branch>
    final String[] jobNameExploded = jobName.split('/')
    if (jobNameExploded == null || jobNameExploded.length != 3) {
        throw new RuntimeException("Unexpected job name format. Expected two '/' chars, actual string: ${jobName}")
    }

    final String repoName = jobNameExploded[1]
    final String gitRepoPrefix = 'platform-services-'

    return repoName.startsWith(gitRepoPrefix) ? repoName.substring(gitRepoPrefix.length()) : repoName
}

/**
 * Get Release Scope
 */
static String getScopeFromBranchName(String branchName) {
    def scope
    if (branchName.contains('minor')) {
        scope = 'minor'
    } else if (branchName.contains('major')) {
        scope = 'major'
    } else {
        scope = 'patch'
    }
    return scope
}

pipeline {

    agent {
        kubernetes {
            label "${getServiceName(decodedJobName)}-${UUID.randomUUID().toString()}".take(53)
            yamlFile 'builderPodTemplate.yaml'
            defaultContainer 'talend-tsbi-springboot-builder'
        }
    }

    environment {
        NEXUS_TPSVC_URL                       = 'https://artifacts-zl.talend.com/nexus/content/repositories/tpsvc'
        NEXUS_TALEND_SNAPSHOTS_URL            = 'https://artifacts-zl.talend.com/nexus/content/repositories/snapshots'
        NEXUS_TALEND_RELEASES_URL             = 'https://artifacts-zl.talend.com/nexus/content/repositories/releases'
        NEXUS_TALEND_OS_RELEASE_URL           = 'https://artifacts-zl.talend.com/nexus/content/repositories/TalendOpenSourceRelease'
        NEXUS_TALEND_OPEN_SOURCE_RELEASE_URL  = 'https://artifacts-zl.talend.com/nexus/content/repositories/TalendOpenSourceRelease'
        NEXUS_TALEND_OPEN_SOURCE_SNAPSHOT_URL = 'https://artifacts-zl.talend.com/nexus/content/repositories/TalendOpenSourceSnapshot'

        PROJECT_TEAM           = 'platform-services'
        GIT_USER               = 'build-talend-tpsvc'
        GIT_EMAIL              = 'build-talend-tpsvc@talend.com'
        GIT_CREDS              = credentials('github-credentials')
        ARTIFACTORY_TALEND     = 'artifactory.datapwn.com/tlnd-docker-dev'
        BUILD_PROFILE          = 'prod'
        BUILD_ENV              = 'ci'
        DOCKER_REGISTRY        = 'artifactory.datapwn.com/tlnd-docker-dev/talend/platform-services'
        DOCKER_IMAGE_NAME_FILE = "docker.image.name.yaml"
    }

    options {
        buildDiscarder(logRotator(artifactNumToKeepStr: '5', numToKeepStr: env.BRANCH_NAME == 'develop' ? '10' : '2'))
        timeout(time: 60, unit: 'MINUTES')
        skipStagesAfterUnstable()
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam(name: 'PUBLISH_SNAPSHOT', defaultValue: false,
                description: 'Do you want to publish a snapshot ?')
    }

    stages {

        stage('Announce Build Start') {
            steps {
                slackSend(color: '#0000AA', channel: "${slackChannel}", message: "STARTED: `${decodedJobName}` #${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)")
            }
        }

        stage('Prepare Git') {
            steps {
                sh '''
                    git version
                    git config --global --add safe.directory "${WORKSPACE}"
                    '''
            }
        }

        stage('Set Gradle options') {
            steps {
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
                        env.RECKON_OPTIONS         = '-Preckon.scope=patch'
                    }
                }
            }
        }

        stage("Pre-Release") {
            when {
                branch releaseBranchPrefix
            }
            steps {
                script {
                    env.RELEASE_SCOPE = getScopeFromBranchName(BRANCH_NAME)
                    echo "Scope of the release is: ${RELEASE_SCOPE}"
                    env.RECKON_OPTIONS = "-Preckon.stage=final -Preckon.scope=${RELEASE_SCOPE}"
                }
                sh '''
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

        stage("Pre-Build") {
            steps {
                gradlew("versionInformation", env.RECKON_OPTIONS)
                script {
                    env.ARTIFACT_VERSION = readFile "version.txt"
                    if (BRANCH_NAME =~ /(PR-.*)/) {
                        env.IMAGE_VERSION = "${ARTIFACT_VERSION}-${BRANCH_NAME}"
                    } else {
                        env.IMAGE_VERSION = env.ARTIFACT_VERSION
                    }
                    env.IMAGE_VERSION_WITH_TIMESTAMP = "${IMAGE_VERSION}-${BUILD_TIMESTAMP}"
                    echo "Project Version is ${ARTIFACT_VERSION} - docker image version is ${IMAGE_VERSION}"
                }
            }
        }

        stage('Build') {
            steps {
                gradlew("clean", "build", env.RECKON_OPTIONS)
            }
        }

        stage("Sonarqube analysis") {
            steps {
                withCredentials([usernamePassword(credentialsId: 'sonar-credentials',
                        passwordVariable: 'SONAR_PASSWORD', usernameVariable: 'SONAR_LOGIN')]) {
                    gradlew("sonarqube",
                            "-x test",
                            "-Dsonar.login='$SONAR_LOGIN'",
                            "-Dsonar.password='$SONAR_PASSWORD'",
                            "-Dsonar.projectVersion='$IMAGE_VERSION'",
                            "-Dsonar.analysisCache.enabled=false",
                            "-Dsonar.host.url=https://sonar.datapwn.com",
                            "-Dsonar.projectKey=${PROJECT_TEAM}:${getServiceName(decodedJobName)}",
                            "-Dsonar.projectName=TPSVC_${getServiceName(decodedJobName)}"
                            , env.RECKON_OPTIONS)
                }
            }
        }

        stage('Release') {
            when {
                anyOf {
                    branch releaseBranchPrefix
                }
            }
            environment {
                GRGIT_USER="$GIT_CREDS_USR"
                GRGIT_PASS="$GIT_CREDS_PSW"
            }
            steps {
                gradlew("publish", "reckonTagPush", env.RECKON_OPTIONS)
                sh "git push --delete origin ${BRANCH_NAME}"
            }
        }

        stage('Publish PR Build') {
            when { branch "PR-*" }
            environment {
                GRGIT_USER="$GIT_CREDS_USR"
                GRGIT_PASS="$GIT_CREDS_PSW"
            }
            steps {
                gradlew("publish", env.RECKON_OPTIONS)
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '*/build/reports/',
                    fingerprint: true, onlyIfSuccessful: true, allowEmptyArchive: true
            archiveArtifacts artifacts: '*/build/reports-test/',
                    fingerprint: true, onlyIfSuccessful: true, allowEmptyArchive: true

            junit testResults: '*/build/reports-test/*/*.xml', allowEmptyResults: true
        }
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
        cleanup {
            sh 'docker logout ${ARTIFACTORY_TALEND}'
        }
    }
}
