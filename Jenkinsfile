// required plugins:
// - OAuth Credentials plugin, org.jenkins-ci.plugins:oauth-credentials:0.4
// - Google Container Registry Auth0, google-container-registry-auth:0.3

def projectVersion
def imageForGradleStages = 'openjdk:11-jdk'
def dockerArgsForGradleStages = '-e GRADLE_USER_HOME=$WORKSPACE/gradle-home -v $HOME/.gradle/caches:/gradle-cache:ro -e GRADLE_RO_DEP_CACHE=/gradle-cache'

pipeline {
    agent none

    options {
        buildDiscarder(logRotator(numToKeepStr: '50', artifactNumToKeepStr: '5'))
    }

    environment {
        // In case the build server exports a custom JAVA_HOME, we fix the JAVA_HOME
        // to the one used by the docker image.
        JAVA_HOME='/usr/local/openjdk-11'
        GRADLE_OPTS='-Dhttp.proxyHost=cache.sernet.private -Dhttp.proxyPort=3128 -Dhttps.proxyHost=cache.sernet.private -Dhttps.proxyPort=3128'
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
                sh './gradlew --no-daemon classes'
            }
        }
        stage('Test') {
            agent {
                docker {
                    image imageForGradleStages
                    args dockerArgsForGradleStages
                }
            }
            steps {
                // Don't fail the build here, let the junit step decide what to do if there are test failures.
                sh script: './gradlew --no-daemon test', returnStatus: true
                // Touch all test results (to keep junit step from complaining about old results).
                sh script: 'find build/test-results | xargs touch'
                junit testResults: 'build/test-results/test/**/*.xml'
                jacoco classPattern: 'build/classes/*/main', sourcePattern: 'src/main'
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
                sh './gradlew --no-daemon build -x test'
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }
        stage('Dockerimage') {
            agent {
                label 'docker-image-builder'
            }
            steps {
                script {
                    def dockerImage = docker.build('eu.gcr.io/veo-projekt/veo-forms', "--label org.opencontainers.image.version='$projectVersion' --label org.opencontainers.image.revision='$env.GIT_COMMIT' .")

                    def removeImage = { identifier ->
                       sh "docker image rm -f ${identifier}"
                    }

                    def pushAndRemoveImage = { img, imageTag ->
                       img.push(imageTag)
                       removeImage("${img.id}:${imageTag}")
                    }

                    // Finally, we'll push the image with several tags:
                    // Pushing multiple tags is cheap, as all the layers are reused.
                    withDockerRegistry(credentialsId: 'gcr:verinice-projekt@gcr', url: 'https://eu.gcr.io') {
                        pushAndRemoveImage(dockerImage, "git-${env.GIT_COMMIT}")
                        if (env.GIT_BRANCH == 'master') {
                            pushAndRemoveImage(dockerImage, 'latest')
                            pushAndRemoveImage(dockerImage, "master-build-${env.BUILD_NUMBER}")
                        } else if (env.GIT_BRANCH ==~ /PR-\d+/) { // we only want to build pull requests
                            // Note that '/' is not allowed in docker tags.
                            def dockertag = env.GIT_BRANCH.replace("/","-")
                            pushAndRemoveImage(dockerImage, dockertag)
                            pushAndRemoveImage(dockerImage, "${dockertag}-build-${env.BUILD_NUMBER}")
                        }
                    }
                    removeImage(dockerImage.id)
                }
            }
        }
        stage('Trigger Deployment') {
            agent any
            when {
                branch 'master'
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
                      excludePattern: 'Jenkinsfile, gradle-home/**, .gradle/**, buildSrc/.gradle/**, */build/**, **/*.pdf, **/*.png, **/*.jpg, **/*.vna'
                    )
                  ]
                )
            }
        }
    }
}
