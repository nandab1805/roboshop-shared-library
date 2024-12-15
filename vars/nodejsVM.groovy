pipeline {
    agent {
        node {
            label 'Agent'
        }
    }
    environment { 
        packageVersion = ''
        nexusURL = '172.31.91.191:8081'
    }
    options {
        timeout(time: 1, unit: 'HOURS') 
        disableConcurrentBuilds()
    }
    parameters {
    // string(name: 'PERSON', defaultValue: 'Mr Jenkins', description: 'Who should I say hello to?')

    // text(name: 'BIOGRAPHY', defaultValue: '', description: 'Enter some information about the person')

    booleanParam(name: 'Deploy', defaultValue: true, description: 'Toggle this value')

    // choice(name: 'CHOICE', choices: ['One', 'Two', 'Three'], description: 'Pick something')

    // password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'Enter a password')
    }
    stages {
        stage('Get the version') {
            steps {
                script {
                    def packageJson = readJSON file: 'package.json'
                    packageVersion = packageJson.version
                    echo "Application version: $packageVersion"
                }
            }
        }
        stage('Install dependencies') {
            steps {
                sh """
                    npm install
                """
            }
        }
        stage('Unit tests') {
            steps {
                sh """
                    echo "unit tests will run here"
                """
            }
        }
        stage('Sonar Scan') {
            steps {
                sh """
                    sonar-scanner
                """
            }
        }
        stage('Build') { // Fixed typo in the name
            steps {
                sh """
                    ls -la
                    zip -q -r catalogue.zip ./* -x ".git" -x "*.zip"
                    ls -ltr
                """
            }
        }
        stage('Publish Artifact') {
            steps {
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: "${nexusURL}",
                    groupId: 'com.roboshop',
                    version: "${packageVersion}",
                    repository: 'catalogue',
                    credentialsId: 'nexus-auth',
                    artifacts: [
                        [
                            artifactId: 'catalogue',
                            classifier: '',
                            file: 'catalogue.zip',
                            type: 'zip'
                        ]
                    ]
                )
            }
        }
        stage('Deploy') {
            when {
                expression{
                    params.Deploy == true
                }
            }
            steps {
                build job: "catalogue-deploy",wait: true, parameters: [
                    string(name: 'version',value:"${packageVersion}" ),
                    string(name: 'environment',value:'dev')
                ]

            }
        }
    }
    post { 
        always { 
            echo 'I will always say Hello again and again!'
            deleteDir()
        }
        failure {
            echo 'This runs when the pipeline fails, used generally to send some alerts.'
        }
        success {
            echo 'I will say Hello when the pipeline is successful.'
        }
    }
}
