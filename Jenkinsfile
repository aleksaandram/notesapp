pipeline {
    agent any

    tools {
        maven 'maven'
    }

    environment {
        NEXUS_URL = 'http://nexus:8081'
        APP_NAME = 'notesapp'
        APP_VERSION = '0.0.1-SNAPSHOT'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building application...'
                sh 'mvn clean package -DskipTests -s settings.xml'
            }
        }

        stage('Test') {
            steps {
                echo 'Running tests...'
                sh 'mvn test -s settings.xml'
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building Docker image...'
                sh 'docker build -t localhost:8082/${APP_NAME}:${APP_VERSION} .'
            }
        }

        stage('Deploy to Nexus') {
            steps {
                echo 'Deploying artifact to Nexus...'
                sh 'mvn deploy -DskipTests -s settings.xml'
            }
        }

        stage('Push Docker Image to Nexus') {
            steps {
                echo 'Pushing Docker image to Nexus...'
                sh '''
                    docker login localhost:8082 -u admin -p Kloi12345
                    docker push localhost:8082/${APP_NAME}:${APP_VERSION}
                '''
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}