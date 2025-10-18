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


        stage('Build image') {
          steps {
            withCredentials([file(credentialsId: 'backend-env-file', variable: 'ENV_FILE')]) {
              bat '''
                setlocal EnableExtensions EnableDelayedExpansion
                for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
                  if not "%%~A"=="" set "%%~A=%%~B"
                )

                if "%IMAGE_TAG%"=="" (
                  for /f %%i in ('git rev-parse --short HEAD ^|^| echo %BUILD_NUMBER%') do set IMAGE_TAG=%%i
                )
                set IMAGE_NAME=%BACK_IMAGE_REPO%:%IMAGE_TAG%

                echo [DEBUG] %IMAGE_NAME%
                if "%BACK_IMAGE_REPO%"=="" exit /b 1
                if "%IMAGE_TAG%"=="" exit /b 1

                docker build -t %IMAGE_NAME% .
              '''
            }
          }
        }

        stage('List image') {
          steps { bat 'docker images' }
        }

        stage('Login & Push image') {
          steps {
            withCredentials([
               file(credentialsId: 'backend-env-file', variable: 'ENV_FILE'),
               usernamePassword(
                  credentialsId: 'docker-hub-creds',
                   usernameVariable: 'DOCKERHUB_USERNAME',
                    passwordVariable: 'DOCKERHUB_PASSWORD'
               )
            ]) {
              bat '''
                setlocal EnableExtensions EnableDelayedExpansion

                REM --- โหลด key=value จากไฟล์ env ---
                for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
                  if not "%%~A"=="" set "%%~A=%%~B"
                )

                REM --- ตั้งค่า IMAGE_TAG ถ้าไม่มากับไฟล์ ให้เอา git short hash ---
                if "%IMAGE_TAG%"=="" (
                  for /f %%i in ('git rev-parse --short HEAD ^|^| echo %BUILD_NUMBER%') do set IMAGE_TAG=%%i
                )

                if "%BACK_IMAGE_REPO%"=="" ( echo [ERR] BACK_IMAGE_REPO is empty & exit /b 1 )
                if "%IMAGE_TAG%"=="" ( echo [ERR] IMAGE_TAG is empty & exit /b 1 )

                set IMAGE_NAME=%BACK_IMAGE_REPO%:%IMAGE_TAG%

                echo [DEBUG] IMAGE_NAME=%IMAGE_NAME%

                echo %DOCKERHUB_PASSWORD% | docker login -u %DOCKERHUB_USERNAME% --password-stdin
                if errorlevel 1 exit /b 1

                docker push %IMAGE_NAME%
              '''
            }
          }
        }

        stage('Deploy Backend to K8s') {
          steps {
            withCredentials([
              file(credentialsId: 'kubeconfig-prod', variable: 'KUBECONFIG_FILE'),
              file(credentialsId: 'backend-env-file', variable: 'ENV_FILE')
            ]) {
              bat """
                REM --- Load env vars from secret file (ถ้ามี) ---
                if exist "%ENV_FILE%" (
                  for /f "usebackq tokens=1,* delims==" %%a in ("%ENV_FILE%") do set %%a=%%b
                )

                REM Guard: ensure variables exist
                if "%K8S_NAMESPACE%"==""   ( echo [ERR] K8S_NAMESPACE not set & exit /b 1 )
                if "%BACK_DEPLOY%"==""     ( echo [ERR] BACK_DEPLOY not set   & exit /b 1 )
                if "%BACK_CONTAINER%"==""  ( echo [ERR] BACK_CONTAINER not set & exit /b 1 )
                if "%BACK_IMAGE_REPO%"=="" ( echo [ERR] BACK_IMAGE_REPO not set & exit /b 1 )
                if "%IMAGE_TAG%"==""       ( echo [ERR] IMAGE_TAG not set & exit /b 1 )

                echo Using: ns=%K8S_NAMESPACE% deploy=%BACK_DEPLOY% container=%BACK_CONTAINER% image=%BACK_IMAGE_REPO%:%IMAGE_TAG%

                REM --- kubeconfig ---
                set KUBECONFIG=%KUBECONFIG_FILE%
                kubectl version --client
                kubectl config current-context

                REM --- Apply manifests ---
                REM ถ้ามี kustomization.yaml ในโฟลเดอร์ k8s ให้ใช้ -k แทน -f
                if exist k8s\\kustomization.yaml (
                  kubectl apply -n %K8S_NAMESPACE% -k k8s\\
                ) else (
                  kubectl apply -n %K8S_NAMESPACE% -f k8s\\deployment.yaml
                  kubectl apply -n %K8S_NAMESPACE% -f k8s\\service-nodeport.yaml
                )

                REM --- อัปเดต image ให้เป็น tag ล่าสุด (สำคัญ) ---
                kubectl -n %K8S_NAMESPACE% set image deploy/%BACK_DEPLOY% %BACK_CONTAINER%=%BACK_IMAGE_REPO%:%IMAGE_TAG%

                REM --- รอ rollout (ยืดเวลาให้ 600s) ---
                kubectl -n %K8S_NAMESPACE% rollout status deploy/%BACK_DEPLOY% --timeout=600s

                if errorlevel 1 (
                  echo === DEBUG: pods (wide) ===
                  kubectl -n %K8S_NAMESPACE% get pods -o wide

                  echo.
                  echo === DEBUG: recent events ===
                  kubectl -n %K8S_NAMESPACE% get events --sort-by=.lastTimestamp | tail -n 80

                  echo.
                  echo === DEBUG: describe deployment ===
                  kubectl -n %K8S_NAMESPACE% describe deploy/%BACK_DEPLOY%

                  echo.
                  echo === DEBUG: first not-ready pod: describe + logs ===
                  for /f "skip=1 tokens=1" %%p in ('kubectl -n %K8S_NAMESPACE% get pods ^| findstr /v "NAME" ^| findstr /v " Running "') do (
                    echo --- POD: %%p ---
                    kubectl -n %K8S_NAMESPACE% describe pod/%%p
                    for /f "tokens=1" %%c in ('kubectl -n %K8S_NAMESPACE% get pod %%p -o jsonpath="{.spec.containers[0].name}"') do (
                      echo --- LOGS (container %%c) ---
                      kubectl -n %K8S_NAMESPACE% logs %%p -c %%c --tail=200
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