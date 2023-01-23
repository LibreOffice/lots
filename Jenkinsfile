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
          if (GIT_BRANCH == 'main' || GIT_BRANCH ==~ 'WollMux_[0-9]*.[0-9]*') {
            withMaven(
              maven: 'mvn',
              mavenLocalRepo: '.repo',
              mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1441715654272',
              publisherStrategy: 'EXPLICIT') {
              withSonarQubeEnv('sonarcloud') {
              sh "mvn $SONAR_MAVEN_GOAL \
                -Dsonar.organization=libreoffice-sonarcloud \
                -Dsonar.projectKey=LibreOffice_WollMux \
                -Dsonar.branch.name=${GIT_BRANCH} \
                -Dsonar.java.source=11 \
                -Dsonar.java.target=11 \
                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                -Dsonar.junit.reportPaths=target/surefire-reports,target/failsafe-reports"
              }
            }
          } else {
            archiveArtifacts artifacts: 'oxt/target/WollMux.oxt'
            withMaven(
              maven: 'mvn',
              mavenLocalRepo: '.repo',
              mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1441715654272',
              publisherStrategy: 'EXPLICIT') {
              withSonarQubeEnv('sonarcloud') {
              sh "mvn $SONAR_MAVEN_GOAL \
                -Dsonar.organization=libreoffice-sonarcloud \
                -Dsonar.projectKey=LibreOffice_WollMux \
                -Dsonar.branch.name=${GIT_BRANCH} \
                -Dsonar.branch.target=${env.CHANGE_TARGET} \
                -Dsonar.java.source=11 \
                -Dsonar.java.target=11 \
                -Dsonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml \
                -Dsonar.junit.reportPaths=target/surefire-reports,target/failsafe-reports"
              }
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
