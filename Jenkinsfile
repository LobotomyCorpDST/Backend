pipeline {
    agent { label 'node' }

    stages {
        stage('Checkout code') {
          steps {
            git branch: 'main', url: 'https://github.com/LobotomyCorpDST/Backend.git'
          }
        }

        stage('Compute Tag') {
          steps {
            bat 'for /f %%i in (\'git rev-parse --short HEAD ^|^| echo %BUILD_NUMBER%\') do echo %%i > tag.txt'
            script { env.IMAGE_TAG = readFile('tag.txt').trim() }
            echo "IMAGE_TAG=${env.IMAGE_TAG}"
          }
        }

        stage('Load Environment Variables') {
          steps {
            withCredentials([
              file(credentialsId: 'backend-env-file', variable: 'ENV_FILE')
            ]) {
              bat """
                REM Load environment variables from secret file
                if exist "%ENV_FILE%" (
                  for /f "usebackq tokens=1,* delims==" %%a in ("%ENV_FILE%") do set %%a=%%b
                )

                REM Export key variables as Jenkins environment variables
                echo BACK_IMAGE_REPO=%BACK_IMAGE_REPO% > env_vars.txt
                echo K8S_NAMESPACE=%K8S_NAMESPACE% >> env_vars.txt
                echo BACK_DEPLOY=%BACK_DEPLOY% >> env_vars.txt
                echo BACK_CONTAINER=%BACK_CONTAINER% >> env_vars.txt
              """
              script {
                def envVars = readFile('env_vars.txt').trim()
                envVars.eachLine { line ->
                  if (line.contains('=')) {
                    def parts = line.split('=', 2)
                    if (parts.size() == 2) {
                      env[parts[0].trim()] = parts[1].trim()
                    }
                  }
                }
                echo "BACK_IMAGE_REPO=${env.BACK_IMAGE_REPO}"
                echo "K8S_NAMESPACE=${env.K8S_NAMESPACE}"
              }
            }
          }
        }

        stage('Build image') {
          steps {
            bat "docker build . -t ${env.BACK_IMAGE_REPO}:${env.IMAGE_TAG}"
          }
        }

        stage('List image') {
          steps { bat 'docker images' }
        }

        stage('Login Docker Hub & Push') {
          steps {
            withCredentials([
              usernamePassword(
                  credentialsId: 'docker-hub-creds',
                  usernameVariable: 'DOCKERHUB_USERNAME',
                  passwordVariable: 'DOCKERHUB_PASSWORD'
              )
            ]) {
              bat """
                docker logout
                echo %DOCKERHUB_PASSWORD% | docker login -u %DOCKERHUB_USERNAME% --password-stdin
                docker push ${env.BACK_IMAGE_REPO}:${env.IMAGE_TAG}
              """
            }
          }
        }

        stage('Deploy Backend to K8s') {
          steps {
            withCredentials([
              file(credentialsId: 'kubeconfig-prod', variable: 'KUBECONFIG_FILE')
            ]) {
              bat """
                REM Guard: ensure variables exist
                if "${env.K8S_NAMESPACE}"==""   ( echo [ERR] K8S_NAMESPACE not set & exit /b 1 )
                if "${env.BACK_DEPLOY}"==""     ( echo [ERR] BACK_DEPLOY not set   & exit /b 1 )
                if "${env.BACK_CONTAINER}"==""  ( echo [ERR] BACK_CONTAINER not set & exit /b 1 )
                if "${env.BACK_IMAGE_REPO}"=="" ( echo [ERR] BACK_IMAGE_REPO not set & exit /b 1 )
                if "${env.IMAGE_TAG}"==""       ( echo [ERR] IMAGE_TAG not set & exit /b 1 )

                echo Using: ns=${env.K8S_NAMESPACE} deploy=${env.BACK_DEPLOY} container=${env.BACK_CONTAINER} image=${env.BACK_IMAGE_REPO}:${env.IMAGE_TAG}

                REM --- kubeconfig ---
                set KUBECONFIG=%KUBECONFIG_FILE%
                kubectl version --client
                kubectl config current-context

                REM --- Apply manifests ---
                if exist k8s\\kustomization.yaml (
                  kubectl apply -n ${env.K8S_NAMESPACE} -k k8s\\
                ) else (
                  kubectl apply -n ${env.K8S_NAMESPACE} -f k8s\\deployment.yaml
                  kubectl apply -n ${env.K8S_NAMESPACE} -f k8s\\service-nodeport.yaml
                )

                REM --- Update image to latest tag ---
                kubectl -n ${env.K8S_NAMESPACE} set image deploy/${env.BACK_DEPLOY} ${env.BACK_CONTAINER}=${env.BACK_IMAGE_REPO}:${env.IMAGE_TAG}

                REM --- Wait for rollout ---
                kubectl -n ${env.K8S_NAMESPACE} rollout status deploy/${env.BACK_DEPLOY} --timeout=600s

                if errorlevel 1 (
                  echo === DEBUG: pods (wide) ===
                  kubectl -n ${env.K8S_NAMESPACE} get pods -o wide

                  echo.
                  echo === DEBUG: recent events ===
                  kubectl -n ${env.K8S_NAMESPACE} get events --sort-by=.lastTimestamp | tail -n 80

                  echo.
                  echo === DEBUG: describe deployment ===
                  kubectl -n ${env.K8S_NAMESPACE} describe deploy/${env.BACK_DEPLOY}

                  echo.
                  echo === DEBUG: first not-ready pod: describe + logs ===
                  for /f "skip=1 tokens=1" %%p in ('kubectl -n ${env.K8S_NAMESPACE} get pods ^| findstr /v "NAME" ^| findstr /v " Running "') do (
                    echo --- POD: %%p ---
                    kubectl -n ${env.K8S_NAMESPACE} describe pod/%%p
                    for /f "tokens=1" %%c in ('kubectl -n ${env.K8S_NAMESPACE} get pod %%p -o jsonpath="{.spec.containers[0].name}"') do (
                      echo --- LOGS (container %%c) ---
                      kubectl -n ${env.K8S_NAMESPACE} logs %%p -c %%c --tail=200
                    )
                    goto :afterlogs
                  )
                  :afterlogs
                  exit /b 1
                )
              """
            }
          }
        }
    }
}