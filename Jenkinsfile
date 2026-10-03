// =====================================================================
// ShopSphere AI - CI/CD pipeline (declarative)
// CI on every branch/PR:  build -> test -> SonarQube + Quality Gate -> OWASP -> Trivy -> Docker
// CD on develop/main:     Nexus -> ECR -> DEV -> smoke test -> approval -> STAGING -> approval -> PROD
// Credentials are referenced by ID only (docs/architecture/conventions.md section 8).
// =====================================================================
pipeline {
  agent any

  options {
    timestamps()
    timeout(time: 60, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '20'))
    disableConcurrentBuilds()
  }

  parameters {
    booleanParam(name: 'SKIP_OWASP', defaultValue: false, description: 'Skip OWASP Dependency-Check (first run downloads the NVD database and is slow)')
  }

  environment {
    AWS_REGION     = 'ap-south-1'
    EKS_CLUSTER    = 'shopsphere-dev'
    NEXUS_URL      = 'http://nexus:8081'
    TRIVY_SEVERITY = 'CRITICAL'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          env.IMAGE_TAG   = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
          env.APP_VERSION = readMavenVersion()
          currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.IMAGE_TAG}"
        }
        echo "Branch=${env.BRANCH_NAME} tag=${env.IMAGE_TAG} version=${env.APP_VERSION}"
      }
    }

    stage('Backend: build & unit tests') {
      steps {
        dir('backend') {
          sh 'chmod +x mvnw && ./mvnw -B -ntp clean verify'
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
          jacoco execPattern: 'backend/target/jacoco.exec', classPattern: 'backend/target/classes', sourcePattern: 'backend/src/main/java'
        }
      }
    }

    stage('Frontend: build & tests') {
      steps {
        dir('frontend') {
          sh 'npm ci --no-audit --no-fund && npm test && npm run build'
        }
      }
    }

    stage('SonarQube analysis') {
      steps {
        dir('backend') {
          withSonarQubeEnv('sonarqube') {   // server named "sonarqube" in Manage Jenkins > System
            sh './mvnw -B -ntp org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar -Dsonar.projectVersion=$APP_VERSION'
          }
        }
      }
    }

    stage('Quality Gate') {
      steps {
        timeout(time: 10, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true   // needs the SonarQube -> Jenkins webhook
        }
      }
    }

    stage('OWASP Dependency-Check') {
      when { expression { !params.SKIP_OWASP } }
      steps {
        withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
          dir('backend') {
            sh './mvnw -B -ntp -Psecurity -DskipTests dependency-check:check'
          }
        }
      }
      post {
        always {
          archiveArtifacts allowEmptyArchive: true, artifacts: 'backend/target/dependency-check-report.*'
        }
      }
    }

    stage('Trivy: filesystem & IaC') {
      steps {
        sh '''
          mkdir -p security/reports
          trivy fs --config security/trivy/trivy.yaml --exit-code 1 --severity $TRIVY_SEVERITY \
               --format table --output security/reports/trivy-fs.txt .
          trivy config --config security/trivy/trivy.yaml --exit-code 0 --severity HIGH,CRITICAL \
               --output security/reports/trivy-iac.txt terraform kubernetes
        '''
      }
      post { always { archiveArtifacts allowEmptyArchive: true, artifacts: 'security/reports/*' } }
    }

    stage('Docker build') {
      steps {
        sh '''
          docker build -t shopsphere-backend:$IMAGE_TAG ./backend
          docker build -t shopsphere-frontend:$IMAGE_TAG \
            --build-arg VITE_BUILD_SHA=$IMAGE_TAG --build-arg VITE_APP_VERSION=$APP_VERSION ./frontend
        '''
      }
    }

    stage('Trivy: images') {
      steps {
        sh '''
          for img in shopsphere-backend shopsphere-frontend; do
            trivy image --config security/trivy/trivy.yaml --exit-code 1 --severity $TRIVY_SEVERITY \
                 --ignore-unfixed --format table --output security/reports/trivy-$img.txt $img:$IMAGE_TAG
          done
        '''
      }
      post { always { archiveArtifacts allowEmptyArchive: true, artifacts: 'security/reports/*' } }
    }

    stage('Publish JAR to Nexus') {
      when { anyOf { branch 'develop'; branch 'main' } }
      steps {
        withCredentials([usernamePassword(credentialsId: 'nexus-credentials', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASSWORD')]) {
          dir('backend') {
            sh './mvnw -B -ntp -s ../jenkins/settings.xml -DskipTests -Dnexus.url=$NEXUS_URL deploy'
          }
        }
      }
    }

    stage('Push images to ECR') {
      when { anyOf { branch 'develop'; branch 'main' } }
      steps {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-jenkins']]) {
          sh '''
            ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
            REGISTRY=$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
            echo "$REGISTRY" > .registry
            aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $REGISTRY
            for img in shopsphere-backend shopsphere-frontend; do
              docker tag $img:$IMAGE_TAG $REGISTRY/$img:$IMAGE_TAG
              docker push $REGISTRY/$img:$IMAGE_TAG
            done
          '''
        }
      }
    }

    stage('Terraform validate') {
      when { anyOf { branch 'develop'; branch 'main' } }
      steps {
        dir('terraform/environments/dev') {
          sh 'terraform init -backend=false -input=false && terraform fmt -check -recursive ../.. && terraform validate'
        }
      }
    }

    stage('Deploy DEV') {
      when { branch 'develop' }
      steps { deployTo('dev') }
    }

    stage('Smoke test DEV') {
      when { branch 'develop' }
      steps { smokeTest('dev') }
    }

    stage('Approve STAGING') {
      when { anyOf { branch 'develop'; branch 'main' } }
      steps {
        timeout(time: 2, unit: 'HOURS') {
          input message: "Promote ${env.IMAGE_TAG} to STAGING?", ok: 'Deploy to STAGING'
        }
      }
    }

    stage('Deploy STAGING') {
      when { anyOf { branch 'develop'; branch 'main' } }
      steps {
        deployTo('staging')
        smokeTest('staging')
      }
    }

    stage('Approve PRODUCTION') {
      when { branch 'main' }
      steps {
        timeout(time: 4, unit: 'HOURS') {
          input message: "Release ${env.IMAGE_TAG} (v${env.APP_VERSION}) to PRODUCTION?", ok: 'Deploy to PROD', submitter: 'admin'
        }
      }
    }

    stage('Deploy PRODUCTION') {
      when { branch 'main' }
      steps {
        deployTo('prod')
        smokeTest('prod')
      }
    }
  }

  post {
    success { echo "SUCCESS: ${env.IMAGE_TAG}" }
    failure { echo 'FAILED - paste the failing stage log into the AI DevOps Assistant (Admin > AI DevOps Assistant).' }
    always  { cleanWs(deleteDirs: true, notFailBuild: true) }
  }
}

// ---------- helpers ----------
def readMavenVersion() {
  return sh(script: "sed -n 's:.*<version>\\(.*\\)</version>.*:\\1:p' backend/pom.xml | sed -n '2p'", returnStdout: true).trim()
}

def deployTo(String environment) {
  withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-jenkins']]) {
    sh """
      aws eks update-kubeconfig --region ${env.AWS_REGION} --name ${env.EKS_CLUSTER}
      kubectl get secret shopsphere-secrets -n shopsphere-${environment} >/dev/null \
        || { echo 'Secret shopsphere-secrets missing in shopsphere-${environment}. Run scripts/deploy.sh secrets ${environment} first.'; exit 1; }
      jenkins/scripts/deploy-k8s.sh ${environment} ${env.IMAGE_TAG} \$(cat .registry)
    """
  }
}

def smokeTest(String environment) {
  withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-jenkins']]) {
    sh """
      HOST=\$(kubectl get ingress shopsphere -n shopsphere-${environment} -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
      jenkins/scripts/smoke-test.sh http://\$HOST
    """
  }
}
