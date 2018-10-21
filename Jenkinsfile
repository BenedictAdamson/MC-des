// Jenkinsfile for the MC-des project

/* 
 * © Copyright Benedict Adamson 2018.
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
            args '-v $HOME/.m2:/root/.m2'
        }
    }
    stages {
        stage('Build') { 
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]){ 
                    sh 'mvn -s $MAVEN_SETTINGS clean package'
                }
            }
        }
        stage('Check') { 
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]){  
                    sh 'mvn -s $MAVEN_SETTINGS spotbugs:spotbugs'
                }
            }
        }
        stage('Test') { 
            steps {
               configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]){   
                   sh 'mvn -s $MAVEN_SETTINGS test'
               }
            }
        }
        stage('Deploy') {
            when {
                anyOf{
                    branch 'develop';
                    branch 'master';
                }
            }
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]){ 
                    sh 'mvn -s $MAVEN_SETTINGS deploy'
                }
            }
        }
    }
    post {
        always {// We ESPECIALLY want the reports on failure
            script {
                def spotbugs = scanForIssues tool: [$class: 'SpotBugs'], pattern: 'target/spotbugsXml.xml'
                publishIssues issues:[spotbugs]
            }
            junit 'target/surefire-reports/**/*.xml' 
        }
    }
}