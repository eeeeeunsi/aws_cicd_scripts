import java.text.SimpleDateFormat

def TODAY = (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date())

pipeline {
    agent any
    environment {
        IMAGE_REGISTRY_URI="685695804727.dkr.ecr.ap-northeast-2.amazonaws.com"
        IMAGE_TAG = "${TODAY}_${BUILD_ID}"
        IMAGE_NAME = "${IMAGE_REGISTRY_URI}/guestbook:${IMAGE_TAG}"
    }

    stages {
        stage('Checkout(CodeCommit)') {
            steps {
                // git branch: 'master', url:'https://github.com/yu3papa/guestbook.git'
                git (branch: 'master'
                    , url: 'https://git-codecommit.ap-northeast-2.amazonaws.com/v1/repos/guestbook'
                    , credentialsId: 'CodeCommit_Credential')
            }
        }
        stage('Build') {
            steps {
                sh './mvnw clean package'
            }
        }
        stage('Unit Test') {
            steps {
                sh './mvnw test'
            }
            
            post {
                always {
                    junit '**/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('Docker Image Build') {
            steps {
                script {
                    //oDockImage = docker.build(IMAGE_NAME)
                    oDockImage = docker.build(IMAGE_NAME, "--build-arg VERSION=${IMAGE_TAG} -f Dockerfile .")
                }
            }
        }
		
        stage('Docker Image Push(ECR)') {
            steps {
                script {
                    docker.withRegistry("https://${IMAGE_REGISTRY_URI}", "ecr:ap-northeast-2:AWS_IAM_administrator_Credential") {
                        oDockImage.push()
                    }
                }
            }
        }

        stage('Staging Deploy') {
            steps {
                sshagent(credentials: ['Staging-PrivateKey']) {
                    sh "ssh -o StrictHostKeyChecking=no ec2-user@172.31.0.110 docker container rm -f guestbookapp"
                    sh "ssh -o StrictHostKeyChecking=no ec2-user@172.31.0.110 docker container run \
                                        -d \
                                        --name=guestbookapp \
                                        --network=host \
                                        -e CONTEXT_PATH=/ \
                                        -e MYSQL_IP=172.31.0.100 \
                                        -e MYSQL_PORT=3306 \
                                        -e MYSQL_DATABASE=guestbook \
                                        -e MYSQL_USER=root \
                                        -e MYSQL_PASSWORD=education \
                                        ${IMAGE_NAME} "
                }
            }
        }
    }
}