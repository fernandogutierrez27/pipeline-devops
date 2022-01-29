/*
	forma de invocación de método call:
	def ejecucion = load 'script.groovy'
	ejecucion.call()
*/
def call(){
  stage("Paso 1: Compliar"){
    sh "mvn clean compile -e"
  }
  stage("Paso 2: Testear"){
​
    sh "mvn clean test -e"
  }
  stage("Paso 3: Build .Jar"){
    sh "mvn clean package -e"
  }
  stage("Paso 4: Sonar - Análisis Estático"){
      sh "echo 'Análisis Estático!'"
      withSonarQubeEnv('sonarqube3') {
          sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=ejemplo-gradle -Dsonar.java.binaries=build'
      }
  }
  stage("Paso 5: Curl Springboot Gradle sleep 20"){
      sh "mvn spring-boot:run&"
      sh "sleep 20 && curl -X GET 'http://localhost:8082/rest/mscovid/test?msg=testing'"
  }
  stage("Paso 6: Subir Nexus"){
      nexusPublisher nexusInstanceId: 'Nexus',
        nexusRepositoryId: 'devops-usach-nexus',
        packages: [
            [
                $class: 'MavenPackage',
                mavenAssetList: [
                    [
                        classifier: '',
                        extension: '',
                        filePath: 'build/DevOpsUsach2022-0.0.0.jar']
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
  stage("Paso 7: Descargar Nexus"){
        withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'USER', passwordVariable: 'PASSWORD')]) {
            sh 'curl -X GET -u $USER:$PASSWORD https://nexus.devopslab.cl/repository/devops-usach-nexus/com/devopsusach2022/DevOpsUsach2022/${ARTIFACT_VERSION}/DevOpsUsach2022-${ARTIFACT_VERSION}.jar -O'
            sh "ls"
        }
    }
  stage("Paso 8: Levantar Artefacto Jar"){
    sh 'nohup java -jar DevOpsUsach2022-${ARTIFACT_VERSION}.jar & >/dev/null'
  }
  stage("Paso 9: Testear Artefacto - Dormir(Esperar 20sg) "){
    sh "sleep 20 && curl -X GET 'http://localhost:8082/rest/mscovid/test?msg=testing'"
  }
}
return this;