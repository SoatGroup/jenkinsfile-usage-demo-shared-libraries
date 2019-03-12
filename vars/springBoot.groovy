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
            M2_REPOSITORY="${WORKSPACE}/${config.folder}/\?/.m2"
            MAVEN_OPTS="-Xmx256m"
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

            stage('PREPARE') {
                steps {
                   sh("""
                   mkdir ${HOME}/.m2 || true
                   chown -R 1000:1000 ${HOME}/.m2
                   """)
                }
            }

            stage('COMPILE') {
                agent {
                    docker {
                        image 'openjdk:11-jdk-slim'
                        args "-v ${HOME}/.m2:${M2_REPOSITORY}"
                        reuseNode true
                    }
                }
                options {
                    timeout(time: 10, unit: 'MINUTES')
                }
                steps {
                    sh("env")
                    sh("cd ${config.folder} && ./mvnw --batch-mode help:effective-settings")
                    sh("cd ${config.folder} && ./mvnw --batch-mode compile")
                    sh("ls -al ${M2_REPOSITORY}/*")
                }
            }

            // stage('TESTS') {
            //     agent {
            //         docker {
            //             image 'openjdk:11-jdk-slim'
            //             args "-v ${HOME}/.m2:${MAVEN_USER_HOME}"
            //             reuseNode true
            //         }
            //     }
            //     options {
            //         timeout(time: 15, unit: 'MINUTES')
            //     }
            //     steps {
            //         sh("cd ${config.folder} && ./mvnw --batch-mode test")
            //     }
            //     post {
            //         always {
            //             junit "${config.folder}/**/TEST-*.xml"
            //         }
            //     }

            // }

            // stage('RELEASE') {
            //     when {
            //         branch 'master'
            //     }
            //     agent {
            //         docker {
            //             image 'openjdk:11-jdk-slim'
            //             args "-v ${HOME}/.m2:${MAVEN_USER_HOME}"
            //             reuseNode true
            //         }
            //     }
            //     options {
            //         timeout(time: 15, unit: 'MINUTES')
            //     }
            //     steps {
            //         sh("cd ${config.folder} && ./mvnw --batch-mode release:prepare release:perform")
            //     }
            //     post {
            //         success {
            //             echo ("A new release have been made !!\nShould send email or a slack notif instead !")
            //         }
            //     }
            // }

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