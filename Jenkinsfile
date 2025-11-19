pipeline {
  agent any

  environment {
    DOCKER_IMAGE = "03chandan/accounts-ms:${BUILD_NUMBER}"
    SONAR_URL = "http://localhost:9000"
  }

  stages {
    stage('Checkout') {
      steps {
        echo 'Checking out code from GitHub...'
        git branch: 'master', url: 'https://github.com/ckumar010398/accounts-ms.git'
      }
    }

    stage('Build and Test') {
      steps {
        echo 'Building and testing with Maven...'
        bat 'mvn clean package -DskipTests=false'
      }
    }

    stage('Static Code Analysis') {
      steps {
        echo 'Running SonarQube analysis...'
        withCredentials([string(credentialsId: 'sonarqube', variable: 'SONAR_AUTH_TOKEN')]) {
          bat '''
            mvn sonar:sonar ^
              -Dsonar.login=%SONAR_AUTH_TOKEN% ^
              -Dsonar.host.url=%SONAR_URL% ^
              -Dsonar.projectKey=accounts-ms ^
              -Dsonar.projectName=accounts-ms
          '''
        }
      }
    }
      stages {
        stage('Build Docker Image') {
          steps {
            bat '''
              docker build -t %DOCKER_IMAGE% .
              docker tag %DOCKER_IMAGE% 03chandan/accounts-ms:latest
            '''
          }
        }
        stage('Push Docker Image') {
          steps {
            withCredentials([usernamePassword(
              credentialsId: 'docker-cred',
              usernameVariable: 'DOCKER_USER',
              passwordVariable: 'DOCKER_PASS'
            )]) {
              bat '''
                docker login -u %DOCKER_USER% -p %DOCKER_PASS%
                docker push %DOCKER_IMAGE%
                docker push 03chandan/accounts-ms:latest
                docker logout
              '''
            }
          }
        }

    stage('Update Deployment File') {
      environment {
        GIT_REPO_NAME = "accounts-ms"
        GIT_USER_NAME = "ckumar010398"
      }
      steps {
        echo 'Updating Kubernetes deployment manifest...'
        withCredentials([string(credentialsId: 'github', variable: 'GITHUB_TOKEN')]) {
          bat '''
            git config user.email "ckumar010398@gmail.com"
            git config user.name "Chandan K"
            powershell -Command "(Get-Content k8s\\deployment.yml) -replace '03chandan/accounts-ms:.*', '03chandan/accounts-ms:%BUILD_NUMBER%' | Set-Content k8s\\deployment.yml"
            git add k8s\\deployment.yml
            git commit -m "Update deployment image to version %BUILD_NUMBER%"
            git push https://%GITHUB_TOKEN%@github.com/%GIT_USER_NAME%/%GIT_REPO_NAME% HEAD:master
          '''
        }
      }
    }
  }

  post {
    always {
      junit allowEmptyResults: true, testResults: 'target\\surefire-reports\\*.xml'
      cleanWs()
    }
    success {
      echo '✅ Pipeline completed successfully!'
      echo "Docker image pushed: ${DOCKER_IMAGE}"
    }
    failure {
      echo '❌ Pipeline failed! Check logs above.'
    }
  }
}
