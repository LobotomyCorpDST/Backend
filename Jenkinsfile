pipeline {
    agent { label 'node' }

    environment {
      K8S_NAMESPACE   = 'doomed-apt'
      BACK_DEPLOY     = 'backend-deployment'
      BACK_CONTAINER  = 'backend'
      BACK_IMAGE_REPO = 'mmmmnl/lobotomy'
    }

    
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
        stage('Deploy Backend to K8s') {
          steps {
            withCredentials([file(credentialsId: 'kubeconfig-prod', variable: 'KUBECONFIG_FILE')]) {
              sh '''
                set -e
                export KUBECONFIG="${KUBECONFIG_FILE}"
                kubectl apply -n ${K8S_NAMESPACE} -f k8s/deployment.yaml
                kubectl apply -n ${K8S_NAMESPACE} -f k8s/service-nodeport.yaml
                kubectl -n ${K8S_NAMESPACE} rollout restart deploy/${BACK_DEPLOY}
                kubectl -n ${K8S_NAMESPACE} rollout status deploy/${BACK_DEPLOY} --timeout=180s
                kubectl -n ${K8S_NAMESPACE} get svc backend -o wide || true
              '''
            }
          }
        }
    }
}
