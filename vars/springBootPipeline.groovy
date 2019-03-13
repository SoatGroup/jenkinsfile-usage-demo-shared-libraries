#!/usr/bin/env groovy
// vars/springBootPipeline.groovy
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

                    Requirements :
                    * Run the vagrant jenkins-slave instance :
                      * Start the server instance : `vagrant up jenkins-slave --no-provision`
                      * Provision the running instance : `vagrant provision jenkins-slave`
                    * Create a new credentials : http://vcap.me:8090/credentials/store/system/domain/_/newCredentials
                      * Kind : SSH Username with private key
                      * ID : 'vagrant-id'
                      * Description : 'vagrant-id'
                      * Username : 'vagrant'
                      * Private key : https://raw.githubusercontent.com/SoatGroup/jenkinsfile-usage-demo/master/vagrant-images/data/server/demo-soat
                    * Create a node : http://vcap.me:8090/computer/new
                      * Node name : 'jenkins-slave'
                      * #2 of executors : 2
                      * Remote root directory : /home/vagrant
                      * Labels : 'vagrant docker'
                      * Launch method : 'Launch agent agents via SSH'
                        * Host : '192.168.10.30'
                        * Credentials : 'vagrant-id'
                        * Host Key Verification Strategy : 'Non verifying Verification Strategy'
                      * Check the `Environment variables`
                        * Add a variable : `USER_HOME` = `/home/vagrant`

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