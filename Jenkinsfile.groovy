import groovy.json.JsonSlurperClassic


node {
    env.AWS_DEFAULT_REGION = 'ap-southeast-1'
	
	def applicationName = 'BandungApp' //change me
	def deploymentGroupName = 'BandungApp' // change me
	def s3BucketName = 'deployment-cdc'
	
	//Cleanup workspace
	deleteDir()

	stage('Checkout') {
		checkout scm	
	}
	
	
	stage("Sonar Analyze") {
		
	}
	

	stage('Deploy') {
		withCredentials([[$class          : 'UsernamePasswordMultiBinding',
                      credentialsId   : 'CdcAWS',
                      usernameVariable: 'AWS_ACCESS_KEY_ID',
                      passwordVariable: 'AWS_SECRET_ACCESS_KEY']]) {
	        
	        

	        //Zip artifact
		def artifactName = "${applicationName}-${deploymentGroupName}"
	        sh("zip -r ${artifactName}.zip .")
	        
	        //Upload artifact to S3
		sh("aws s3 cp ${artifactName}.zip s3://${s3BucketName}/${artifactName}.zip")

	        //Create Deployment
	        def result = sh(returnStdout:true, script: "aws deploy create-deployment --application-name ${applicationName} --deployment-group-name ${deploymentGroupName} --s3-location bucket=${s3BucketName},bundleType=zip,key=${artifactName}.zip")	        
	        def json = parseJson(result)
	        String deploymentId = json.deploymentId

	        //Wait until success
	        sh("aws deploy wait deployment-successful --deployment-id ${deploymentId}")
	    }
	}	
}

def parseJson(String json) {
	return new JsonSlurperClassic().parseText(json)
}
