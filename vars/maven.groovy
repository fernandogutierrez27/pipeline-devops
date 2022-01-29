/*
	forma de invocación de método call:
	def ejecucion = load 'script.groovy'
	ejecucion.call()
*/
def call(){

    env.STAGE = "Paso 1: Compliar"
    stage("$env.STAGE") {
        sh "mvn clean compile -e"
    }

    env.STAGE = "Paso 2: Testear"
    stage("$env.STAGE") {
        sh "mvn clean test -e"
    }

    env.STAGE = "Paso 3: Build .Jar"
    stage("$env.STAGE") {
        sh "mvn clean package -e"
    }

    env.STAGE = "Paso 4: Sonar - Análisis Estático"
    stage("$env.STAGE") {
        sh "echo 'Análisis Estático!'"
        withSonarQubeEnv('SonarQubeUsach') {
            sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=ejemplo-gradle -Dsonar.java.binaries=build'
        }
    }

    env.STAGE = "Paso 5: Curl Springboot Maven sleep 20"
    stage("$env.STAGE") {
        sh "mvn spring-boot:run&"
        sh "sleep 20 && curl -X GET 'http://localhost:8082/rest/mscovid/test?msg=testing'"
    }

    env.STAGE = "Paso 6: Subir Nexus"
    stage("$env.STAGE") {
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

    env.STAGE = "Paso 7: Descargar Nexus"
    stage("$env.STAGE") {
        withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'USER', passwordVariable: 'PASSWORD')]) {
            sh 'curl -X GET -u $USER:$PASSWORD https://nexus.devopslab.cl/repository/devops-usach-nexus/com/devopsusach2022/DevOpsUsach2022/${ARTIFACT_VERSION}/DevOpsUsach2022-${ARTIFACT_VERSION}.jar -O'
            sh "ls"
        }
    }

    env.STAGE = "Paso 8: Levantar Artefacto "
    stage("$env.STAGE") {
        sh 'nohup java -jar DevOpsUsach2022-${ARTIFACT_VERSION}.jar & >/dev/null'
    }

    env.STAGE = "Paso 9: Testear Artefacto - Dormir(Esperar 20sg"
    stage("$env.STAGE") {
        sh "sleep 20 && curl -X GET 'http://localhost:8082/rest/mscovid/test?msg=testing'"
    }

}
return this;