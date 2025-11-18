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
        sh 'mvn clean package -DskipTests=false'
      }
    }

    stage('Static Code Analysis') {
      steps {
        withCredentials([string(credentialsId: 'sonarqube', variable: 'SONAR_AUTH_TOKEN')]) {
          sh '''
            mvn sonar:sonar \
              -Dsonar.login=$SONAR_AUTH_TOKEN \
              -Dsonar.host.url=${SONAR_URL} \
              -Dsonar.projectKey=accounts-ms \
              -Dsonar.projectName=accounts-ms
          '''
        }
      }
    }

    stage('Build Docker Image') {
      steps {
        echo 'Building Docker image...'
        sh 'docker build -t ${DOCKER_IMAGE} .'
      }
    }

    stage('Push Docker Image') {
      steps {
        echo 'Pushing Docker image to registry...'
        script {
          docker.withRegistry('https://index.docker.io/v1/', "docker-cred") {
            docker.image("${DOCKER_IMAGE}").push()
            docker.image("${DOCKER_IMAGE}").push('latest')
          }
        }
      }
    }

    stage('Update Deployment File') {
      environment {
        GIT_REPO_NAME = "accounts-ms"
        GIT_USER_NAME = "ckumar010398"
      }
      steps {
        withCredentials([string(credentialsId: 'github', variable: 'GITHUB_TOKEN')]) {
          sh '''
            git config user.email "ckumar010398@gmail.com"
            git config user.name "Chandan K"
            sed -i "s/replaceImageTag/${BUILD_NUMBER}/g" deployment.yml
            git add deployment.yml
            git commit -m "Update deployment image to version ${BUILD_NUMBER}"
            git push https://${GITHUB_TOKEN}@github.com/${GIT_USER_NAME}/${GIT_REPO_NAME} HEAD:master
          '''
        }
      }
    }
  }

  post {
    always {
      junit 'target/surefire-reports/*.xml'
      cleanWs()
    }
    success {
      echo 'Pipeline completed successfully!'
    }
    failure {
      echo 'Pipeline failed!'
    }
  }
}
