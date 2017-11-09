#!groovy
@Library("platform.infrastructure.jenkinslib")
import java.lang.Object

node("docker") {

    // Create a groovy library object from the Shared.groovy file.
    def shared = new com.ebsco.platform.Shared()

    // Checkout the repo from github
    stage('checkout') {
    	// Ensure we start with an empty directory.
    	deleteDir()
        checkout scm
    }

    /*
     * Run gradle build tasks -- gradle assemble, test, build
     * Please ensure that gradle build invokes all of your gradle tasks
     * Stages: Compile, Test, Build
     */
    stage('build') {
    	shared.gradleBuild()
    }

    stage('publish') {
    	withCredentials([usernamePassword(credentialsId: 'artifactory-jenkins', usernameVariable: 'ArtifactoryUser', passwordVariable: 'ArtifactoryPassword')]) {
            sh '/opt/gradle/bin/gradle publishToMavenLocal artifactoryPublish'
        }
    }
}