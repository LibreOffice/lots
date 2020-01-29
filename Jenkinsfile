#!/usr/bin/env groovy

pipeline {
  agent { 
    label 'wollmux'
  }
	
  options {
    disableConcurrentBuilds()
  }
	
  stages {
    stage('Build') {
      steps {
        withMaven(
          maven: 'mvn',
          mavenLocalRepo: '.repo',
          mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1441715654272',
          publisherStrategy: 'EXPLICIT') {
          sh "mvn clean package"
        }
      }
    }
    stage('Quality Gate') {
      steps {
        script {
          if (GIT_BRANCH == 'master' || GIT_BRANCH == 'WollMux_18.1') {
            withMaven(
              maven: 'mvn',
              mavenLocalRepo: '.repo',
              mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1441715654272',
              publisherStrategy: 'EXPLICIT') {
              withSonarQubeEnv('SonarQube') {
                sh "mvn $SONAR_MAVEN_GOAL \
                  -Dsonar.host.url=$SONAR_HOST_URL \
                  -Dsonar.branch.name=${GIT_BRANCH}"
              }
            }
          } else {
            withMaven(
              maven: 'mvn',
              mavenLocalRepo: '.repo',
              mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1441715654272',
              publisherStrategy: 'EXPLICIT') {
	          withSonarQubeEnv('SonarQube') {
	            sh "mvn $SONAR_MAVEN_GOAL \
	              -Dsonar.host.url=$SONAR_HOST_URL \
	              -Dsonar.branch.name=${GIT_BRANCH} \
	              -Dsonar.branch.target=${env.CHANGE_TARGET} "
	          }
            }
            timeout(time: 1, unit: 'HOURS') {
             waitForQualityGate abortPipeline: true
            }
            archiveArtifacts artifacts: 'dist/WollMux.oxt', onlyIfSuccessful: true
          }
        }
      }
    }
  }
}
