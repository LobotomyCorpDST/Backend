pipeline {
    agent { label 'node' }
    
    stages {
        stage('Checkout code') {
            steps {
                git branch: 'main', url: 'https://github.com/LobotomyCorpDST/Backend.git'
            }
        }
        stage('Build') {
            steps {
                sh 'docker build . -t mmmmnl/lobotomycorp:v.0.0'
            }
        }
        stage('List image') {
            steps {
                sh 'docker images'
            }
        }
        stage('Login Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-creds',
                                                 usernameVariable: 'DOCKERHUB_USERNAME',
                                                 passwordVariable: 'DOCKERHUB_PASSWORD')]) {
                    sh '''
                        echo $DOCKERHUB_PASSWORD | docker login -u $DOCKERHUB_USERNAME --password-stdin
                    '''
                }
            }
        }
        stage('Push image') {
            steps {
                sh 'docker push mmmmnl/lobotomycorp:v.0.0'
            }
        }
    }
}
