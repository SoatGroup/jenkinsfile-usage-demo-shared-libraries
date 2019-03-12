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
            MVNW_VERBOSE="true"
            MAVEN_USER_HOME="${WORKSPACE}/${config.folder}/.m2"
            MAVEN_OPTS="-Xmx256m"
            // MAVEN_OPTS="-Dmaven.user.home=${WORKSPACE}/.m2 -Xmx256m"
        }

        stages {
            stage('DESCRIPTION') {
                steps {
                    echo("""
                    Sample of spring boot application :

                    Shared library variables :
                    * folder : ${config.folder}

                    Pipeline description :
                    * COMPILE : Compile application
                    * TESTS : Launch tests
                    * RELEASE : Execute release (only on master branch)
                    """)
                }
            }

            stage('PREPARE') {
                steps {
                   sh("mkdir ${HOME}/.m2 && chown -R 1000:1000 ${HOME}/.m2")
                }
            }

            stage('COMPILE') {
                agent {
                    docker {
                        image 'openjdk:11-jdk-slim'
                        args "-v ${HOME}/.m2:${MAVEN_USER_HOME}:rw"
                        reuseNode true
                    }
                }
                options {
                    timeout(time: 10, unit: 'MINUTES')
                }
                steps {
                    echo "${MAVEN_USER_HOME}"
                    sh("pwd")
                    sh("ls -al ${MAVEN_USER_HOME}")
                    sh("touch ${MAVEN_USER_HOME}/test.toto")
                    sh("ls -al ${MAVEN_USER_HOME}")

                    sh("cd ${config.folder} && ./mvnw --batch-mode help:effective-settings")
                    sh("ls -al /home/vagrant/workspace/app-shared-libraries_feature_wip/sample-spring-boot-app/?/.m2/wrapper/dists/apache-maven-3.6.0-bin/*")
                    sh("ls -al /home/vagrant/workspace/app-shared-libraries_feature_wip/sample-spring-boot-app/?/.m2/wrapper/dists/apache-maven-3.6.0-bin/*/*")
                }
            }

            // stage('TESTS') {
            //     agent {
            //         docker {
            //             image 'openjdk:11-jdk-slim'
            //             args '-v $HOME/.m2:/root/.m2'
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
            //             args '-v $HOME/.m2:/root/.m2'
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