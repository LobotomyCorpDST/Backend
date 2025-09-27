pipeline {
    agent {label 'node'}
    
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
        stage('login docker hub') {
            steps {
                sh 'docker login -u ${{ secrets.DOCKERHUB_USERNAME }} -p ${{ secrets.DOCKERHUB_TOKEN }}'
            }
        }
        stage('Push image') {
            steps {
                sh 'docker push mmmmnl/lobotomycorp:v.0.0'
            }
        }
    }
}
