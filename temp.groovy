node {
  def MVN_HOME
  stage('init') {
    echo "Current workspace : ${WORKSPACE}"
    MVN_HOME = tool 'maven3'
  }
  stage('project-clone'){
    git (
      branch: "master“,
      credentialsId: "${codeCommitCredentialsId}",
      url: "https://git-codecommit.ap-northeast-2.amazonaws.com/v1/repos/demo"
    )
}
  stage('project-build’){
    sh "'${MVN_HOME}/bin/mvn' clean install -DskipTests=true"
  }
stage('deploy'){
    fileOperations([
      // 배포 디렉토리 삭제
      folderDeleteOperation('target/deploy'),
      // 배포 디렉토리 생성
      folderCreateOperation('target/deploy'),
      // CodeDeploy 에서 사용할 scripts 디렉토리 복사 
      folderCopyOperation(sourceFolderPath: 'scripts', destinationFolderPath: 'target/deploy/scripts'),
      
      // 빌드된 Application 의 결과물 복사
      fileCopyOperation(excludes: '', flattenFiles: true, includes: 'target/demo-*.jar', targetLocation: 'target/deploy'),
      
      // CodeDeploy 에서 사용할 appspec 파일 복사
      fileCopyOperation(excludes: '', flattenFiles: true, includes: 'appspec.yml', targetLocation: 'target/deploy’)
    ])
    dir("./target/deploy"){
      withCredentials([[$class:    'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: "${awsCredentialsId}",  secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]){
      step([$class: 'AWSCodeDeployPublisher’,
        // CodeDeploy Application 명
        applicationName: 'demo-app',
        // AWS Credentials Access Key ID
        awsAccessKey: "${AWS_ACCESS_KEY_ID}",
        // AWS Credentials Secret Key
        awsSecretKey: "${AWS_SECRET_ACCESS_KEY}",
        credentials: 'awsAccessKey',
        // CodeDeploy 배포그룹에 명시된 배포 방식 유형
        deploymentConfig: 'CodeDeployDefault.OneAtATime',
        deploymentGroupAppspec: false,
        // CodeDeploy 배포그룹명
        deploymentGroupName: 'demo-app-group',
        excludes: '',
        iamRoleArn: '',
        // 배포 어플리케이션의 Artifact 위치
        includes: '**',
        proxyHost: '',
        proxyPort: 0,
        // Region 명
        region: 'ap-northeast-2',
        // Archive 될 S3 Bucket 명
        s3bucket: 'chiseok-codedeploy-archive',
        // Archive 될 S3 Bucket 경로
        s3prefix: "deploy",
        subdirectory: '',
        versionFileName: '',
        // CodeDeploy 가 수행 완료 될때 까지의 대기여부 설정
        waitForCompletion: true,
        // CodeDeploy 가 수행 완료 될때 까지의 Timeout
        pollingTimeoutSec: 1800
      ])
    }
  }
}