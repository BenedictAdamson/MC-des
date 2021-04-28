// Jenkinsfile for the MC-des project

/* 
 * © Copyright Benedict Adamson 2018-21.
 * 
 * This file is part of MC-des.
 *
 * MC-des is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MC-des is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MC-des.  If not, see <https://www.gnu.org/licenses/>.
 */
 
 /*
  * Jenkins plugins used:
  * Config File Provider
  *     - Should configure the file settings.xml with ID 'maven-settings' as the Maven settings file
  * JUnit
  * Warnings 5
  */
 
pipeline { 
    agent {
        dockerfile {
            filename 'Jenkins.Dockerfile'
            args '-v $HOME/.m2:/root/.m2 --network="host"'
        }
    }
    triggers {
        pollSCM('H */4 * * *')
    }
    environment {
        JAVA_HOME = '/usr/lib/jvm/java-11-openjdk-amd64'
    }
    stages {
        stage('Clean') { 
            steps {
               sh 'rm -rf "$WORKSPACE/target"'
            }
        }
        stage('Build and verify') {
            when{
                not{
                    branch 'master'
                }
            } 
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]){ 
                    sh 'mvn -B -s $MAVEN_SETTINGS verify'
                }
            }
        }
        stage('Build, verify and deploy') {
            when{
                 branch 'master'
            } 
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]){ 
                    sh 'mvn -B -s $MAVEN_SETTINGS deploy'
                }
            }
        }
    }
    post {
        always {// We ESPECIALLY want the reports on failure
            script {
                recordIssues tools: [
                	java(),
                	javaDoc(),
                	mavenConsole(),
                	pmdParser(pattern: '**/target/pmd.xml'),
					spotBugs(pattern: '**/target/spotbugsXml.xml')
					]
            }
            junit 'target/*-reports/**/TEST-*.xml' 
        }
        success {
            archiveArtifacts artifacts: 'target/MC-des-*.jar', fingerprint: true
        }
    }
}