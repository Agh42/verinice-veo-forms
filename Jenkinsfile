// required plugins:
// - OAuth Credentials plugin, org.jenkins-ci.plugins:oauth-credentials:0.4
// - Google Container Registry Auth0, google-container-registry-auth:0.3

def projectVersion
def imageForGradleStages = 'openjdk:17-jdk-bullseye'
def dockerArgsForGradleStages = '-v /data/gradle-homes/executor-$EXECUTOR_NUMBER:/gradle-home -e GRADLE_USER_HOME=/gradle-home'

def withDockerNetwork(Closure inner) {
  try {
    networkId = UUID.randomUUID().toString()
    sh "docker network create ${networkId}"
    inner.call(networkId)
  } finally {
    sh "docker network rm ${networkId}"
  }
}

pipeline {
    agent none

    options {
        buildDiscarder(logRotator(numToKeepStr: '50', artifactNumToKeepStr: '5'))
    }

    environment {
        // In case the build server exports a custom JAVA_HOME, we fix the JAVA_HOME
        // to the one used by the docker image.
        JAVA_HOME='/usr/local/openjdk-17'
        GRADLE_OPTS='-Dhttp.proxyHost=cache.sernet.private -Dhttp.proxyPort=3128 -Dhttps.proxyHost=cache.sernet.private -Dhttps.proxyPort=3128'
        // pass -Pci=true to gradle, https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
        ORG_GRADLE_PROJECT_ci=true
    }

    stages {
        stage('Setup') {
            agent {
                docker {
                    image imageForGradleStages
                    args dockerArgsForGradleStages
                }
            }
            steps {
                sh 'env'
                buildDescription "${env.GIT_BRANCH} ${env.GIT_COMMIT[0..8]}"
                script {
                    projectVersion = sh(returnStdout: true, script: '''./gradlew properties -q | awk '/^version:/ {print $2}' ''').trim()
                }
            }
        }
        stage('Build') {
            agent {
                docker {
                    image imageForGradleStages
                    args dockerArgsForGradleStages
                }
            }
            steps {
                sh './gradlew --no-daemon classes generateLicenseReport'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'build/reports/dependency-license/*.*', allowEmptyArchive: true
                }
            }
        }
        stage('Test') {
            agent any
            steps {
                 script {
                     withDockerNetwork{ n ->
                         docker.image('postgres:11.7-alpine').withRun("--network ${n} --name database-${n} -e POSTGRES_USER=test -e POSTGRES_PASSWORD=test") { db ->
                             docker.image(imageForGradleStages).inside("${dockerArgsForGradleStages} --network ${n} -e SPRING_DATASOURCE_URL=jdbc:postgresql://database-${n}:5432/postgres -e SPRING_DATASOURCE_DRIVERCLASSNAME=org.postgresql.Driver") {
                                // Don't fail the build here, let the junit step decide what to do if there are test failures.
                                 sh script: './gradlew --no-daemon test'
                                 // Touch all test results (to keep junit step from complaining about old results).
                                 sh script: 'find build/test-results | xargs touch'
                                 junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
                                 jacoco classPattern: '**/build/classes/java/main'
                             }
                         }
                     }
                 }
            }
        }
        stage('Artifacts') {
            agent {
                docker {
                    image imageForGradleStages
                    args dockerArgsForGradleStages
                }
            }
            steps {
                sh './gradlew --no-daemon -PciBuildNumer=$BUILD_NUMBER -PciJobName=$JOB_NAME build -x test'
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }
        stage('Validate 3rd party license report') {
            agent {
                docker {
                    image imageForGradleStages
                    alwaysPull true
                    args dockerArgsForGradleStages
                }
            }
            steps {
                unarchive mapping: ['build/reports/dependency-license/LICENSE-3RD-PARTY.txt': 'build/reports/dependency-license/LICENSE-3RD-PARTY.txt']
                script {
                    def repositoryFileContent = readFile('LICENSE-3RD-PARTY.txt')
                    def generatedFileContent = readFile('build/reports/dependency-license/LICENSE-3RD-PARTY.txt')
                    def repositoryFileContentWithoutDate = repositoryFileContent
                        .replaceAll(/\RThis report was generated at .+\R/, '')
                    def generatedFileContentWithoutDate = generatedFileContent
                        .replaceAll(/\RThis report was generated at .+\R/, '')
                    if (repositoryFileContentWithoutDate != generatedFileContentWithoutDate){
                        error 'LICENSE-3RD-PARTY.txt is not up to date, please re-run ./gradlew generateLicenseReport'
                    }
                }
            }
        }
        stage('Dockerimage') {
            agent {
                label 'docker-image-builder'
            }
            steps {
                script {
                    def dockerImage = docker.build("eu.gcr.io/veo-projekt/veo-forms:git-${env.GIT_COMMIT}", "--build-arg VEO_FORMS_VERSION='$projectVersion' --label org.opencontainers.image.version='$projectVersion' --label org.opencontainers.image.revision='$env.GIT_COMMIT' .")
                    // Finally, we'll push the image with several tags:
                    // Pushing multiple tags is cheap, as all the layers are reused.
                    withDockerRegistry(credentialsId: 'gcr:verinice-projekt@gcr', url: 'https://eu.gcr.io') {
                        dockerImage.push("git-${env.GIT_COMMIT}")
                        if (env.GIT_BRANCH == 'master') {
                            dockerImage.push(projectVersion)
                            dockerImage.push("latest")
                            dockerImage.push("master-build-${env.BUILD_NUMBER}")
                        } else if (env.GIT_BRANCH == 'develop') {
                            dockerImage.push("develop")
                            dockerImage.push("develop-build-${env.BUILD_NUMBER}")
                        }
                    }
                }
            }
        }
        stage('Trigger Deployment') {
            agent any
            when {
                anyOf { branch 'master'; branch 'develop' }
            }
            steps {
                build job: 'verinice-veo-deployment/master'
            }
        }
    }
    post {
        always {
           node('') {
                recordIssues(enabledForFailure: true, tools: [java()])
                recordIssues(
                  enabledForFailure: true,
                  tools: [
                    taskScanner(
                      highTags: 'FIXME',
                      ignoreCase: true,
                      normalTags: 'TODO',
                      excludePattern: 'Jenkinsfile, gradle/wrapper/**, gradle-home/**, .gradle/**, buildSrc/.gradle/**, build/**'
                    )
                  ]
                )
            }
        }
    }
}
