# Tukma-Backend

Tukma is a research project designed to filter and select suitable job applicants.

## Overview

The Tukma application provides a platform for:
- Recruiters to post job opportunities and review applicant resumes
- Applicants to search for jobs and submit their resumes
- AI-powered resume analysis to match applicants with job requirements
- Automated interviewing capabilities

## API Documentation

The API is organized into several logical sections:

- [Authentication API](docs/auth-api.md) - User registration, login, and status
- [Jobs API](docs/jobs-api.md) - Job creation, management, search, and retrieval
- [Resume API](docs/resume-api.md) - Resume uploads, analysis, and retrieval
- [Interviewer API](docs/interviewer-api.md) - Real-time interviewing via WebSockets
- [General Information](docs/general-info.md) - Authentication, models, and common patterns

## Technology Stack

- **Backend**: Spring Boot 3.4.1
- **Database**: PostgreSQL
- **Authentication**: JWT (JSON Web Tokens)
- **Cache**: Redis
- **External APIs**: OpenAI for text and audio processing
- **Deployment**: Docker

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL
- Redis
- OpenAI API key

### Installation

1. Clone the repository:
```bash
git clone https://github.com/your-username/tukma-backend.git
```

2. Configure environment variables:
Create a `src/main/resources/application.properties` file with the following content:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/tukma
spring.datasource.username=your_username
spring.datasource.password=your_password

# Redis Configuration
spring.data.redis.url=redis://localhost:6379

# JWT Secret
jwt.secret=your_jwt_secret_key

# OpenAI API Key
openai.key=your_openai_api_key
```

3. Build the application:
```bash
./mvnw clean package
```

4. Run the application:
```bash
./mvnw spring-boot:run
```

The API will be available at http://localhost:8080

## Docker Deployment

To build and run with Docker:

```bash
docker buildx build --platform linux/amd64 -t tukma-backend .
docker run -p 8080:8080 tukma-backend
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
