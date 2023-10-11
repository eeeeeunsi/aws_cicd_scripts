import java.text.SimpleDateFormat

def TODAY = (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date())

pipeline {
    agent any

    environment {
        IMAGE_REGISTRY_URI="685695804727.dkr.ecr.ap-northeast-2.amazonaws.com"
        IMAGE_TAG = "${TODAY}_${BUILD_ID}"
    }

    stages {
        // stage('AWS CodeBuild') {
        //     steps {
        //         awsCodeBuild(
        //             credentialsType: 'keys',
        //             credentialsId: 'CodeBuild_Credential',
        //             region: 'ap-northeast-2',
        //             projectName: 'guestbook',
        //             sourceControlType: 'project',
        //             sseAlgorithm: 'AES256',
        //             envVariables: "[ { IMAGE_TAG, ${IMAGE_TAG} } ]",
        //             buildSpecFile: "buildspec.yml",
        //             artifactTypeOverride: "S3",
        //             artifactNamespaceOverride: "NONE",
        //             artifactPackagingOverride: "ZIP",
        //             artifactPathOverride: "${currentBuild.number}",
        //             artifactLocationOverride: "yu3papa-20230317-guestbook"
        //         )
        //     }
        // }

        stage('AWS CodeDeploy') {
            steps {
                script {
                    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: "CodeBuild_Credential",  secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]){
                        step([$class: 'AWSCodeDeployPublisher',

                            awsAccessKey: "${AWS_ACCESS_KEY_ID}", // AWS Credentials Access Key ID
                            awsSecretKey: "${AWS_SECRET_ACCESS_KEY}", // AWS Credentials Secret Key
                            credentials: 'awsAccessKey',
                            applicationName: 'guestbook', // CodeDeploy 어플리케이션명
                            deploymentGroupName: 'stage-deploy-group', // CodeDeploy 배포그룹명
                            deploymentConfig: 'CodeDeployDefault.OneAtATime', // CodeDeploy 배포그룹에 명시된 배포 방식 유형
                            region: 'ap-northeast-2', // Region 명
                            
                            deploymentGroupAppspec: false,
                            // excludes: '',
                            iamRoleArn: '',
                            // includes: '**', // 배포 어플리케이션의 Artifact 위치
                            proxyHost: '',
                            proxyPort: 0,

                            s3bucket: 'yu3papa-20230317-guestbook', // Archive 될 S3 Bucket 명
                            s3prefix: "13", // Archive 될 S3 Bucket 경로
                            subdirectory: '',
                            versionFileName: 'guestbook',
                            waitForCompletion: true, // CodeDeploy 가 수행 완료 될때 까지의 대기여부 설정
                            pollingTimeoutSec: 60 // CodeDeploy 가 수행 완료 될때 까지의 Timeout
                        ])
                    }
                }
            }
        }
    }
}