/*
	forma de invocación de método call:
	def ejecucion = load 'script.groovy'
	ejecucion.call()
*/

import groovy.json.JsonSlurperClassic
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}

def call(){

    pipeline {
        agent any

        triggers {
            githubPush()
        }

        environment {
            ARTIFACT_VERSION = '0.0.7'
        }

        stages {
            stage("Paso 1: Build && Test"){
                steps {
                    sh "echo 'Build && Test!'"
                    sh "gradle clean build"
                }
            }
            stage("Paso 2: Sonar - Análisis Estático"){
                steps {
                    sh "echo 'Análisis Estático!'"
                    withSonarQubeEnv('SonarQubeUsach') {
                        sh "echo 'SonarQube Analysis!'"
                        // Run Maven on a Unix agent.
                        sh 'gradle sonarqube -Dsonar.projectKey=ejemplo-gradle -Dsonar.java.binaries=build'
                    }
                }
            }
            stage("Paso 3: Curl Springboot Gradle sleep 20"){
                steps {
                    sh "gradle bootRun&"
                    sh "sleep 20 && curl -X GET 'http://localhost:8082/rest/mscovid/test?msg=testing'"
                }
            }
            stage("Paso 4: Subir Nexus"){
                steps {
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
            }
            stage("Paso 5: Descargar Nexus"){
                steps {
                    withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'USER', passwordVariable: 'PASSWORD')]) {
                        sh 'curl -X GET -u $USER:$PASSWORD https://nexus.devopslab.cl/repository/devops-usach-nexus/com/devopsusach2022/DevOpsUsach2022/${ARTIFACT_VERSION}/DevOpsUsach2022-${ARTIFACT_VERSION}.jar -O'
                        sh "ls"
                    }
                }
            }
            stage("Paso 6: Levantar Artefacto Jar"){
                steps {
                    sh 'nohup java -jar DevOpsUsach2022-${ARTIFACT_VERSION}.jar & >/dev/null'
                }
            }
            stage("Paso 7: Testear Artefacto - Dormir(Esperar 20sg) "){
                steps {
                    sh "sleep 20 && curl -X GET 'http://localhost:8082/rest/mscovid/test?msg=testing'"
                }
            }
        }
        post {
            always {
                sh "echo 'fase always executed post'"
            }
            success {
                sh "echo 'fase success'"
            }
            failure {
                sh "echo 'fase failure'"
            }
        }
    }

}

return this;