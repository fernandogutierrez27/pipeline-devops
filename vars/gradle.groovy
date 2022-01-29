def call() {
    stage("Paso 1: Build && Test"){
        sh "echo 'Build && Test!'"
        sh "gradle clean build"
    }
    stage("Paso 2: Sonar - Análisis Estático"){
        sh "echo 'Análisis Estático!'"
        withSonarQubeEnv('SonarQubeUsach') {
            sh "echo 'SonarQube Analysis!'"
            // Run Maven on a Unix agent.
            sh 'gradle sonarqube -Dsonar.projectKey=ejemplo-gradle -Dsonar.java.binaries=build'
        }
    }
    stage("Paso 3: Curl Springboot Gradle sleep 20"){
        sh "gradle bootRun&"
        sh "sleep 20 && curl -X GET 'http://localhost:8082/rest/mscovid/test?msg=testing'"
    }
    stage("Paso 4: Subir Nexus"){
        nexusPublisher nexusInstanceId: 'Nexus',
        nexusRepositoryId: 'devops-usach-nexus',
        packages: [
            [
                $class: 'MavenPackage',
                mavenAssetList: [
                    [
                        classifier: '',
                        extension: '',
                        filePath: 'build/libs/DevOpsUsach2022-0.0.0.jar']
                    ],
                mavenCoordinate: [
                    artifactId: 'DevOpsUsach2022',
                    groupId: 'com.devopsusach2022',
                    packaging: 'jar',
                    version: ARTIFACT_VERSION
                ]
            ]
        ]
    }
    stage("Paso 5: Descargar Nexus"){
        withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'USER', passwordVariable: 'PASSWORD')]) {
            sh 'curl -X GET -u $USER:$PASSWORD https://nexus.devopslab.cl/repository/devops-usach-nexus/com/devopsusach2022/DevOpsUsach2022/${ARTIFACT_VERSION}/DevOpsUsach2022-${ARTIFACT_VERSION}.jar -O'
            sh "ls"
        }
    }
    stage("Paso 6: Levantar Artefacto Jar"){
        sh 'nohup java -jar DevOpsUsach2022-${ARTIFACT_VERSION}.jar & >/dev/null'
    }
    stage("Paso 7: Testear Artefacto - Dormir(Esperar 20sg) "){
        sh "sleep 20 && curl -X GET 'http://localhost:8082/rest/mscovid/test?msg=testing'"
    }
}