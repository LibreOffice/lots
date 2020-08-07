#!/usr/bin/env groovy

pipeline {
  agent { 
    label 'wollmux'
  }
	
  options {
    disableConcurrentBuilds()
  }

  tools {
    jdk 'Java11'
  }

  environment {
    UNO_PATH = '/opt/libreoffice6.4/program/'
  }
	
  stages {
    stage('Build') {
      steps {
        withMaven(
          maven: 'mvn',
          mavenLocalRepo: '.repo',
          mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1441715654272',
          publisherStrategy: 'EXPLICIT') {
          sh "mvn -DskipTests -DdryRun clean package"
        }
      }
    }

    stage('Junit') {
      steps {
        withMaven(
          maven: 'mvn',
          mavenLocalRepo: '.repo',
          mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1441715654272',
          publisherStrategy: 'EXPLICIT') {
          sh "mvn -Dmaven.javadoc.skip=true -DdryRun test verify"
        }
      }
    }

    stage('Quality Gate') {
      steps {
        script {
          if (GIT_BRANCH == 'master' || GIT_BRANCH ==~ 'WollMux_[0-9]*.[0-9]*') {
            withMaven(
              maven: 'mvn',
              mavenLocalRepo: '.repo',
              mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1441715654272',
              publisherStrategy: 'EXPLICIT') {
              withSonarQubeEnv('SonarQube') {
              sh "mvn $SONAR_MAVEN_GOAL \
                -Dsonar.projectKey=de.muenchen:wollmux \
                -Dsonar.branch.name=${GIT_BRANCH} \
                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                -Dsonar.junit.reportPaths=target/surefire-reports"
              }
            }
          } else {
            archiveArtifacts artifacts: 'target/WollMux.oxt'
            withMaven(
              maven: 'mvn',
              mavenLocalRepo: '.repo',
              mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1441715654272',
              publisherStrategy: 'EXPLICIT') {
              withSonarQubeEnv('SonarQube') {
              sh "mvn $SONAR_MAVEN_GOAL \
                -Dsonar.projectKey=de.muenchen:wollmux \
                -Dsonar.branch.name=${GIT_BRANCH} \
                -Dsonar.branch.target=${env.CHANGE_TARGET} \
                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                -Dsonar.junit.reportPaths=target/surefire-reports"
              }
            }
            timeout(time: 1, unit: 'HOURS') {
              waitForQualityGate abortPipeline: true
            }
          }
        }
      }
    }
  }

  post {
    cleanup {
      cleanWs()
    }
  }
}
