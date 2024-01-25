pipeline {
    agent any

    tools {
        jdk 'A408_BE_Build'
    }
    environment {
        CONTAINER_NAME = "auto-dev-server"
        SSH_CREDENTIALS = 'DevOps'
        REMOTE_HOST = 'ssafyhelper.shop'
        SCRIPT_PATH = '/temp/AutoDevServer.sh'
        SSH_REMOTE_CONFIG = 'ubuntu'
    }

    stages {

        stage('Build BE') {
            steps {
                script {
                    dir('BE') {
                        sh 'chmod +x gradlew'
                        sh 'ls -l'

                        sh './gradlew clean build'
                        sh 'jq --version'
                        sh 'cd build/libs && ls -al'

                        
                    }
                }
            }
        }
        stage('Send Artifact'){
            sshPublisher(
                            publishers: [
                                sshPublisherDesc(
                                    configName: 'ssafyhelper',
                                    transfers: [
                                        sshTransfer(
                                            sourceFiles: '/BE/build/libs/goumunity-0.0.1-SNAPSHOT.jar',
                                            removePrefix: '/build/libs',
                                            remoteDirectory: '/sendData',
                                            execCommand: 'sh temp/AutoDevServer.sh'
                                        )
                                    ]
                                )
                            ]
                        )
        }
    }

    post {
        success {
            echo 'Build successful!'
        }
        failure {
            echo 'Build failed!'
        }
    }
}
