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
          if (GIT_BRANCH == 'master') {
            withMaven(
              maven: 'mvn',
              mavenLocalRepo: '.repo',
              mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1441715654272',
              publisherStrategy: 'EXPLICIT') {
              withSonarQubeEnv('SonarQube') {
                sh "mvn $SONAR_MAVEN_GOAL -Dsonar.host.url=$SONAR_HOST_URL"
              }
            }
            timeout(time: 1, unit: 'HOURS') {
              script {
                def qg = waitForQualityGate()
                if (qg.status != 'OK') {
                  error "Pipeline abgebrochen auf Grund von Quality Gate Fehlern: ${qg.status}"
                }
              }
            }
          } else {
            withMaven(
              maven: 'mvn',
              mavenLocalRepo: '.repo',
              mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1441715654272',
              publisherStrategy: 'EXPLICIT') {
              withSonarQubeEnv('SonarQube') {
                withCredentials([usernamePassword(credentialsId: '3eaee9fd-bbdd-4825-a4fd-6b011f9a84c3', passwordVariable: 'GITHUB_ACCESS_TOKEN', usernameVariable: 'USER')]) {
                    sh "mvn $SONAR_MAVEN_GOAL \
                      -Dsonar.host.url=$SONAR_HOST_URL \
                      -Dsonar.analysis.mode=preview \
                      -Dsonar.github.pullRequest=${env.CHANGE_ID} \
                      -Dsonar.github.repository=wollmux/wollmux \
                      -Dsonar.github.oauth=${GITHUB_ACCESS_TOKEN}"
                }
              }
            }
          }
        }
      }
    }
  }
}
