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

        environment {
            MAVEN_OPTS="-Duser.home=${USER_HOME} -Xmx256m"
        }

        stages {
            stage('DESCRIPTION') {
                steps {
                    echo("""
                    Sample of spring boot application :

                    Shared library variables :
                    * folder : ${config.folder}

                    Pipeline description :
                    * PREPARE : Ensure .m2 cache directory exist
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
                        args "-v ${USER_HOME}/.m2:${USER_HOME}/.m2"
                        reuseNode true
                    }
                }
                options {
                    timeout(time: 10, unit: 'MINUTES')
                }
                steps {
                    sh("ls ${USER_HOME}/.m2/*")
                    sh("cd ${config.folder} && ./mvnw --batch-mode help:effective-settings")
                    sh("cd ${config.folder} && ./mvnw --batch-mode compile")
                }
            }

            stage('TESTS') {
                agent {
                    docker {
                        image 'openjdk:11-jdk-slim'
                        args "-v ${USER_HOME}/.m2:${USER_HOME}/.m2"
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
                    beforeAgent true
                    branch 'master'
                }
                agent {
                    docker {
                        image 'openjdk:11-jdk-slim'
                        args "-v ${USER_HOME}/.m2:${USER_HOME}/.m2"
                        reuseNode true
                    }
                }
                options {
                    timeout(time: 15, unit: 'MINUTES')
                }
                steps {
                    sh("cd ${config.folder} && ./mvnw --batch-mode release:prepare release:perform")
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