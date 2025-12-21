# AWS Deployment Guide - EC2 with Docker Container

This guide demonstrates deploying the **Event Service** as a Docker container on an AWS EC2 instance.

## 📋 Prerequisites

- AWS Account
- AWS CLI installed and configured
- Docker installed locally
- Java 21 installed
- Gradle installed (or use gradlew wrapper)

---

## Part A: Prepare Docker Image Locally

### Step 1: Build the Event Service Application

Navigate to the Event Service directory:
- Go to the `event-service` folder in your project
- Ensure all dependencies are resolved

Build the application using Gradle:
- Run the Gradle build command to compile and package the application
- Skip tests to speed up the build process
- Verify the JAR file is created in the `build/libs` directory
- Note the JAR filename (e.g., `event-service-0.0.1-SNAPSHOT.jar`)

### Step 2: Create Dockerfile

Create a `Dockerfile` in the event-service directory with the following configuration:

**Dockerfile structure:**
- Use Java 21 runtime as the base image (e.g., `openjdk:21-jdk-slim`)
- Set the working directory to `/app`
- Copy the JAR file from `build/libs` to the container
- Expose port 8085
- Define the entry point to run the JAR file with Java

**Example Dockerfile content:**
```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY build/libs/event-service-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 3: Build Docker Image Locally

Build the Docker image:
- Use Docker build command with a tag name `shadow-ledger/event-service:latest`
- Run the build from the event-service directory
- Wait for all layers to download and build
- Verify the image is created successfully

Test the image locally (optional but recommended):
- Run the container locally with appropriate environment variables
- Map port 8085 to host
- Set environment variables for database and Kafka
- Verify the service starts without errors
- Test the health endpoint
- Stop the test container

---

## Part B: Deploy to AWS

### Step 1: Configure AWS CLI

Configure your AWS credentials using the AWS CLI:
- Set up your Access Key ID and Secret Access Key
- Choose your preferred region (e.g., us-east-1)
- Set output format to JSON
- Verify your configuration to ensure proper authentication

---

## Step 2: Create Amazon ECR Repository

Create an Elastic Container Registry (ECR) repository to store your Docker images:
- Create a repository named `shadow-ledger/event-service` in your chosen region
- Note down the repository URI from the output
- This URI will be used to push and pull Docker images

Authenticate Docker to your ECR repository:
- Use AWS CLI to get login credentials for ECR
- Authenticate your local Docker client with these credentials

---

## Step 3: Tag and Push Docker Image to ECR

Tag the Docker image for ECR:
- Tag your locally built image (`shadow-ledger/event-service:latest`) with the ECR repository URI
- Use the repository URI obtained from Step 2
- The tag should include your AWS account ID and region

Push the Docker image to ECR:
- Push the tagged image to your ECR repository
- Monitor the upload progress
- Verify the image appears in your ECR repository with correct tags and size

---

## Step 4: Create EC2 Security Group

Create a security group to control network access:
- Create a security group named `shadow-ledger-sg`
- Add a description for the security group
- Note the Security Group ID from the output

Configure inbound rules:
- Allow SSH access on port 22 from anywhere (for management)
- Allow HTTP access on port 8085 (Event Service port)
- These rules enable remote access and service connectivity

---

## Step 5: Create SSH Key Pair

Generate an SSH key pair for secure EC2 access:
- Create a key pair named `shadow-ledger-key`
- Save the private key material to a .pem file
- Set appropriate file permissions (read-only for owner)
- Keep this key file secure - it's needed for SSH access

---

## Step 6: Launch EC2 Instance

Prepare a user data script:
- Create a shell script that will run on instance launch
- Include commands to update the system
- Install Docker
- Start Docker service
- Add the default user to the docker group
- Enable Docker to start on boot

Launch the EC2 instance:
- Use Amazon Linux 2 AMI
- Select t2.micro instance type (Free Tier eligible)
- Attach the SSH key pair created earlier
- Associate the security group
- Attach the user data script
- Add appropriate tags for identification
- Note the Instance ID from the output

Get the public IP address:
- Wait 1-2 minutes for the instance to initialize
- Query the instance details to get the public IP address
- Save this IP address for SSH access and testing

---

## Step 7: Deploy Container on EC2

Connect to your EC2 instance:
- Use SSH with your private key file
- Connect to the instance using the public IP address

Install AWS CLI on EC2:
- Install AWS CLI on the EC2 instance
- Configure AWS credentials (same as your local credentials)
- Authenticate Docker to ECR from the EC2 instance

Pull and run the Docker container:
- Pull the Event Service image from ECR
- Run the container with appropriate configuration:
  - Map port 8085 from container to host
  - Set environment variables for database connection (host, port, name, user, password)
  - Set Kafka bootstrap servers environment variable
  - Configure container to restart automatically
  - Run container in detached mode
- Use your local machine's public IP for database and Kafka connections

Verify deployment:
- Check that the container is running
- View container logs to ensure no errors
- Test the health endpoint locally on EC2

---

## Step 8: Verify External Access

Test from your local machine:
- Access the health endpoint using the EC2 public IP
- Verify the service returns a healthy status

Update API Gateway configuration:
- Modify the API Gateway configuration to point to the EC2 public IP
- Restart the API Gateway service
- Test event submission through the gateway
- Verify events are processed correctly

---
