#!/usr/bin/env groovy
// vars/springBoot.groovy
def call(Map config) {

    pipeline {

        agent { label 'docker' }

        options {
            timestamps()
            skipStagesAfterUnstable()
            disableConcurrentBuilds()
            timeout(time: 40, unit: 'MINUTES')
            buildDiscarder(logRotator(numToKeepStr: '10'))
        }

        stages {
            stage('DESCRIPTION') {
                steps {
                    echo("""
                    Sample of spring boot application :

                    Requirements :
                    * [Recommended] Set new credentials : ${JENKINS_URL}credentials/store/system/domain/_/newCredentials
                    * Type : username & password
                    * ID : 'github-id'
                    * Username : YOUR_GITHUB_USER
                    * Password : YOUR_GITHUB_PASSWORD

                    Shared library variables :
                    * folder : ${config.folder}
                    * credentials : ${config.credentialsId}
                    * repository : ${config.repository}

                    Pipeline description :
                    * COMPILE : Compile application
                    * TESTS : Launch tests
                    * RELEASE : Execute release (only on master branch)
                    """)
                }
            }

            stage('COMPILE') {
                agent {
                    docker {
                        image 'openjdk:11-jdk-slim'
                        args '-v $HOME/.m2:/root/.m2'
                        reuseNode true
                    }
                }
                options {
                    timeout(time: 10, unit: 'MINUTES')
                }
                steps {
                    sh("cd ${config.folder} && ./mvnw --batch-mode compile")
                }
            }

            stage('TESTS') {
                agent {
                    docker {
                        image 'openjdk:11-jdk-slim'
                        args '-v $HOME/.m2:/root/.m2'
                        reuseNode true
                    }
                }
                options {
                    timeout(time: 15, unit: 'MINUTES')
                }
                steps {
                    sh("cd ${config.folder} && ./mvnw --batch-mode test")
                }
                post {
                    always {
                        junit "${config.folder}/**/TEST-*.xml"
                    }
                }

            }

            stage('RELEASE') {
                when {
                    branch 'master'
                }
                agent {
                    docker {
                        image 'openjdk:11-jdk-slim'
                        args '-v $HOME/.m2:/root/.m2'
                        reuseNode true
                    }
                }
                options {
                    timeout(time: 15, unit: 'MINUTES')
                }
                steps {
                    sh("cd ${folder} && ./mvnw --batch-mode release:prepare release:perform")
                }
                post {
                    success {
                        echo ("A new release have been made !!\nShould send email or a slack notif instead !")
                    }
                }
            }

        }

        post {
            always {
                cleanWs()
            }
            failure {
                echo ("Something was wrong !!\nShould send email or a slack notif instead !")
            }
        }

    }






}