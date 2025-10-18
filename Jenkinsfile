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

            REM --- load key=value from env file safely ---
            if exist "%ENV_FILE%" (
              for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
                if not "%%~A"=="" set "%%~A=%%~B"
              )
            )

            REM prefer IMAGE_TAG from Jenkins env; if empty, derive from git/BUILD_NUMBER
            if "%IMAGE_TAG%"=="" (
              for /f %%i in ('git rev-parse --short HEAD ^|^| echo %BUILD_NUMBER%') do set IMAGE_TAG=%%i
            )

            if "%BACK_IMAGE_REPO%"=="" ( echo [ERR] BACK_IMAGE_REPO is empty & exit /b 1 )
            if "%IMAGE_TAG%"=="" ( echo [ERR] IMAGE_TAG is empty & exit /b 1 )

            set "IMAGE_NAME=%BACK_IMAGE_REPO%:%IMAGE_TAG%"

            echo( [DEBUG] IMAGE_NAME=%IMAGE_NAME%
            docker build -t %IMAGE_NAME% .
          '''
        }
      }
    }

    stage('List image') {
      steps {
        bat 'docker images'
      }
    }

    stage('Login & Push image') {
      steps {
        withCredentials([
          file(credentialsId: 'backend-env-file', variable: 'ENV_FILE'),
          usernamePassword(credentialsId: 'docker-hub-creds', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_PASSWORD')
        ]) {
          bat '''
            setlocal EnableExtensions EnableDelayedExpansion

            REM --- load key=value from env file safely ---
            if exist "%ENV_FILE%" (
              for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
                if not "%%~A"=="" set "%%~A=%%~B"
              )
            )

            if "%IMAGE_TAG%"=="" (
              for /f %%i in ('git rev-parse --short HEAD ^|^| echo %BUILD_NUMBER%') do set IMAGE_TAG=%%i
            )

            if "%BACK_IMAGE_REPO%"=="" ( echo [ERR] BACK_IMAGE_REPO is empty & exit /b 1 )
            if "%IMAGE_TAG%"=="" ( echo [ERR] IMAGE_TAG is empty & exit /b 1 )

            set "IMAGE_NAME=%BACK_IMAGE_REPO%:%IMAGE_TAG%"
            echo( [DEBUG] IMAGE_NAME=%IMAGE_NAME%

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
          bat '''
            setlocal EnableExtensions EnableDelayedExpansion

            REM --- load key=value from env file safely ---
            if exist "%ENV_FILE%" (
              for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
                if not "%%~A"=="" set "%%~A=%%~B"
              )
            )

            REM guard required vars
            if "%K8S_NAMESPACE%"==""   ( echo [ERR] K8S_NAMESPACE not set & exit /b 1 )
            if "%BACK_DEPLOY%"==""     ( echo [ERR] BACK_DEPLOY not set   & exit /b 1 )
            if "%BACK_CONTAINER%"==""  ( echo [ERR] BACK_CONTAINER not set & exit /b 1 )
            if "%BACK_IMAGE_REPO%"=="" ( echo [ERR] BACK_IMAGE_REPO not set & exit /b 1 )
            if "%IMAGE_TAG%"==""       ( echo [ERR] IMAGE_TAG not set & exit /b 1 )

            set "KUBECONFIG=%KUBECONFIG_FILE%"
            echo( Using: ns=%K8S_NAMESPACE% deploy=%BACK_DEPLOY% container=%BACK_CONTAINER% image=%BACK_IMAGE_REPO%:%IMAGE_TAG%
            kubectl version --client
            kubectl config current-context

            REM --- apply manifests ---
            if exist k8s\\kustomization.yaml (
              kubectl apply -n %K8S_NAMESPACE% -k k8s\\
            ) else (
              kubectl apply -n %K8S_NAMESPACE% -f k8s\\deployment.yaml
              kubectl apply -n %K8S_NAMESPACE% -f k8s\\service-nodeport.yaml
            )

            REM --- ensure image tag ---
            kubectl -n %K8S_NAMESPACE% set image deploy/%BACK_DEPLOY% %BACK_CONTAINER%=%BACK_IMAGE_REPO%:%IMAGE_TAG%

            REM --- wait for rollout ---
            kubectl -n %K8S_NAMESPACE% rollout status deploy/%BACK_DEPLOY% --timeout=600s

            if errorlevel 1 (
              echo(
              echo( === DEBUG: pods (wide) ===
              kubectl -n %K8S_NAMESPACE% get pods -o wide

              echo(
              echo( === DEBUG: recent events (last 80) ===
              kubectl -n %K8S_NAMESPACE% get events --sort-by=.metadata.creationTimestamp > events.txt
              powershell -NoProfile -Command "Get-Content -Path 'events.txt' -Tail 80"

              echo(
              echo( === DEBUG: describe deployment ===
              kubectl -n %K8S_NAMESPACE% describe deploy/%BACK_DEPLOY%

              echo(
              echo( === DEBUG: first not-ready pod: describe + logs ===
              powershell -NoProfile -Command ^
                "$ErrorActionPreference='Stop';" ^
                "$ns=$env:K8S_NAMESPACE; $dep=$env:BACK_DEPLOY; $ctr=$env:BACK_CONTAINER;" ^
                "$pods = (kubectl -n $ns get pods -l app=backend -o json | ConvertFrom-Json).items;" ^
                "if(-not $pods){ Write-Host 'No pods found for app=backend'; exit 1 }" ^
                "$notReady = $pods | Where-Object { $_.status.containerStatuses -and ($_.status.containerStatuses | Where-Object { -not $_.ready }) } | Select-Object -First 1;" ^
                "if(-not $notReady){ $notReady = $pods | Select-Object -First 1 }" ^
                "Write-Host '--- POD:' $notReady.metadata.name;" ^
                "kubectl -n $ns describe pod $($notReady.metadata.name);" ^
                "Write-Host ('--- LOGS (container {0}) ---' -f $ctr);" ^
                "kubectl -n $ns logs $($notReady.metadata.name) -c $ctr --tail=200"

              exit /b 1
            )
          '''
        }
      }
    }
  }
}
