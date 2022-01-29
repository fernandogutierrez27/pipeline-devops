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
        environment {
            ARTIFACT_VERSION = '2.0.0'
        }
        parameters {
            choice(
                name:'compileTool',
                choices: ['Maven', 'Gradle'],
                description: 'Seleccione herramienta de compilacion'
            )
        }
        stages {
            stage("Pipeline"){
                steps {
                    script{
                        switch(params.compileTool) {
                            case 'Maven':
                                maven()
                            break;
                            case 'Gradle':
                                gradle()
                            break;
                        }
                    }
                }
                post{
                    success{
                        slackSend color: 'good', message: "[Fernando Gutierrez] [${JOB_NAME}] [${BUILD_TAG}] Ejecucion Exitosa", teamDomain: 'dipdevopsusac-tr94431', tokenCredentialId: 'slack-personal'
                    }
                    failure{
                        slackSend color: 'danger', message: "[Fernando Gutierrez] [${env.JOB_NAME}] [${BUILD_TAG}] Ejecucion fallida en stage [${env.STAGE}]", teamDomain: 'dipdevopsusac-tr94431', tokenCredentialId: 'slack-personal'
                    }
                }
            }
        }
    }

}

return this;