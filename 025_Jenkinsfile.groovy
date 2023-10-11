import java.text.SimpleDateFormat

def TODAY = (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date())

pipeline {
    agent any

    environment {
        IMAGE_REGISTRY_URI="685695804727.dkr.ecr.ap-northeast-2.amazonaws.com"
        IMAGE_TAG = "${TODAY}_${BUILD_ID}"
        ARTIFACT_S3 = "yu3papa-20230317-cicd-guestbook"
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
                    buildSpecFile: "codebuild/buildspec.yml",
                    artifactTypeOverride: "S3",
                    artifactNamespaceOverride: "NONE",
                    artifactPackagingOverride: "ZIP",
                    artifactPathOverride: "${currentBuild.number}",
                    artifactLocationOverride: "${ARTIFACT_S3}"
                )
            }
        }

        stage('AWS CodeDeploy') {
            steps {
                script {
                    sh"""
                        aws deploy create-deployment \
                            --application-name guestbook \
                            --deployment-group-name stage-deploy-group \
                            --region ap-northeast-2 \
                            --s3-location bucket=${ARTIFACT_S3},bundleType=zip,key=${currentBuild.number}/guestbook \
                            --file-exists-behavior OVERWRITE \
                            --output json > DEPLOYMENT_ID.json
                    """

                    def DEPLOYMENT_ID = sh(script: "cat DEPLOYMENT_ID.json | grep -o '\"deploymentId\": \"[^\"]*' | cut -d'\"' -f4", returnStdout: true).trim()
                    echo "${DEPLOYMENT_ID}"
                    sh "rm -fr ./DEPLOYMENT_ID.json"
                    def DEPLOYMENT_RESULT = ""
                    while("$DEPLOYMENT_RESULT" != "\"Succeeded\"") {
                        DEPLOYMENT_RESULT = sh(
                            script:"aws deploy get-deployment \
                                        --region ap-northeast-2 \
                                        --query \"deploymentInfo.status\" \
                                        --deployment-id ${DEPLOYMENT_ID}",
                            returnStdout: true
                        ).trim()
                        echo "$DEPLOYMENT_RESULT"
                        if("$DEPLOYMENT_RESULT" == "\"Failed\""){
                            currentBuild.result = 'FAILURE'
                            break
                        }
                        sleep(10)
                    }
                    currentBuild.result = 'SUCCESS'
                }
            }
        }
    }
}