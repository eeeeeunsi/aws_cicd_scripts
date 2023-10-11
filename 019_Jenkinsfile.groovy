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
        stage('AWS CodeBuild') {
            steps {
                awsCodeBuild(
                    credentialsType: 'keys',
                    credentialsId: 'AWS_IAM_administrator_Credential',
                    region: 'ap-northeast-2',
                    projectName: 'guestbook',
                    sourceControlType: 'project',
                    sseAlgorithm: 'AES256',
                    envVariables: "[ { IMAGE_TAG, ${IMAGE_TAG} } ]",
                    buildSpecFile: "codebuild/buildspec.yml"
                )
            }
        }

        stage('Staging Deploy') {
            steps {
                sshagent(credentials: ['Staging-PrivateKey']) {
                    sh "ssh -o StrictHostKeyChecking=no ec2-user@172.31.0.110 docker container rm -f guestbookapp"
                    sh "ssh -o StrictHostKeyChecking=no ec2-user@172.31.0.110  aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 685695804727.dkr.ecr.ap-northeast-2.amazonaws.com"
                    sh "ssh -o StrictHostKeyChecking=no ec2-user@172.31.0.110 \
                                        docker container run \
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