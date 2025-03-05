# Tukma-Backend

Tukma is a research project designed to filter and select suitable job applicants.

## API Documentation

This document provides a comprehensive guide to the Tukma API endpoints.

### Authentication

#### Sign Up

```
POST /api/v1/auth/signup
```

Creates a new user account.

**Request Body (for Applicants):**
```json
{
  "email": "applicant@example.com",        // required
  "password": "password123",             // required
  "firstName": "John",                  // required
  "lastName": "Doe",                    // required
  "isApplicant": true                     // required
}
```

**Request Body (for Recruiters):**
```json
{
  "email": "recruiter@example.com",       // required
  "password": "password123",             // required
  "firstName": "Jane",                  // required
  "lastName": "Smith",                  // required
  "isApplicant": false,                   // required
  "companyName": "Acme Inc."             // required for recruiters
}
```

**Response:**
- `200 OK`: Account created successfully
- `409 Conflict`: User already exists
- `500 Internal Server Error`: Server error

#### Login

```
POST /api/v1/auth/login
```

Authenticate a user and get a JWT token.

**Request Body:**
```json
{
  "email": "user@example.com",           // required
  "password": "password123"              // required
}
```

**Response:**
```json
{
  "Message": "Login Successful",
  "ticket": "abc123xyz456"
}
```

A JWT token is also sent as an HTTP-only cookie.

#### User Status

```
GET /api/v1/auth/user-status
```

Get the current authenticated user's details.

**Response:**
```json
{
  "userDetails": {
    "id": 1,
    "username": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "isRecruiter": false,
    "companyName": "Acme Inc." // Only present for recruiter accounts
  }
}
```

### Jobs

#### Create Job

```
POST /api/v1/jobs/create-job
```

Create a new job posting.

**Request Body:**
```json
{
  "title": "Software Engineer",              // required
  "description": "We are looking for...",     // required
  "address": "123 Main Street, SF, CA",       // required
  "type": "FULL_TIME",                        // required
  "shiftType": "DAY_SHIFT",                   // optional
  "shiftLengthHours": 8,                       // optional
  "locationType": "ON_SITE",                  // required
  "keywords": ["java", "spring", "api"]      // optional
}
```

**Response:**
```json
{
  "id": 1,
  "owner": {
    "id": 1,
    "username": "recruiter@example.com",
    "firstName": "Jane",
    "lastName": "Recruiter",
    "isRecruiter": true
  },
  "description": "We are looking for a software engineer...",
  "title": "Software Engineer",
  "address": "123 Main Street, San Francisco, CA 94105",
  "accessKey": "abc-1234",
  "type": "FULL_TIME",
  "shiftType": "DAY_SHIFT",
  "shiftLengthHours": 8,
  "createdAt": "2025-02-26T10:00:00",
  "updatedAt": "2025-02-26T10:00:00"
}
```

#### Get All Jobs (Deprecated)

```
GET /api/v1/jobs/get-jobs
```

Get all jobs created by the authenticated user, including associated keywords.

**Note:** This endpoint is deprecated. Please use the paginated endpoint (`/api/v1/jobs/get-jobs-owner`) instead.

**Response:**
```json
[
  {
    "job": {
      "id": 1,
      "owner": {
        "id": 1,
        "username": "recruiter@example.com",
        "firstName": "Jane",
        "lastName": "Recruiter",
        "isRecruiter": true
      },
      "description": "We are looking for a software engineer...",
      "title": "Software Engineer",
      "address": "123 Main Street, San Francisco, CA 94105",
      "accessKey": "abc-1234",
      "type": "FULL_TIME",
      "shiftType": "DAY_SHIFT",
      "shiftLengthHours": 8,
      "createdAt": "2025-02-26T10:00:00",
      "updatedAt": "2025-02-26T10:00:00"
    },
    "keywords": ["java", "spring", "api", "microservices"]
  },
  {
    "job": {
      "id": 2,
      "owner": {
        "id": 1,
        "username": "recruiter@example.com",
        "firstName": "Jane",
        "lastName": "Recruiter",
        "isRecruiter": true
      },
      "description": "Looking for a product manager with 3+ years of experience...",
      "title": "Product Manager",
      "address": "456 Market Street, San Francisco, CA 94105",
      "accessKey": "def-5678",
      "type": "FULL_TIME",
      "shiftType": "FLEXIBLE_SHIFT",
      "shiftLengthHours": 8,
      "createdAt": "2025-02-25T15:30:00",
      "updatedAt": "2025-02-25T15:30:00"
    },
    "keywords": ["product management", "agile", "leadership"]
  }
]
```

#### Get Paginated Jobs (Recruiter View)

```
GET /api/v1/jobs/get-jobs-owner
```

Get paginated jobs created by the authenticated user, sorted by most recently updated first. This endpoint is intended for recruiters to view their own job postings.

**Parameters:**
- `page` (optional): The page number (0-based, defaults to 0)
- `size` (optional): The number of items per page (defaults to 10)

**Request Example:**
```
GET /api/v1/jobs/get-jobs-owner?page=0&size=5
```

**Response:**
```json
{
  "jobs": [
    {
      "job": {
        "id": 1,
        "owner": {
          "id": 1,
          "username": "recruiter@example.com",
          "firstName": "Jane",
          "lastName": "Recruiter",
          "isRecruiter": true
        },
        "description": "We are looking for a software engineer...",
        "title": "Software Engineer",
        "address": "123 Main Street, San Francisco, CA 94105",
        "accessKey": "abc-1234",
        "type": "FULL_TIME",
        "shiftType": "DAY_SHIFT",
        "shiftLengthHours": 8,
        "createdAt": "2025-02-26T10:00:00",
        "updatedAt": "2025-03-04T15:30:00"
      },
      "keywords": ["java", "spring", "api", "microservices"]
    },
    {
      "job": {
        "id": 2,
        "owner": {
          "id": 1,
          "username": "recruiter@example.com",
          "firstName": "Jane",
          "lastName": "Recruiter",
          "isRecruiter": true
        },
        "description": "Looking for a product manager with 3+ years of experience...",
        "title": "Product Manager",
        "address": "456 Market Street, San Francisco, CA 94105",
        "accessKey": "def-5678",
        "type": "FULL_TIME",
        "shiftType": "FLEXIBLE_SHIFT",
        "shiftLengthHours": 8,
        "createdAt": "2025-02-25T15:30:00",
        "updatedAt": "2025-02-25T15:30:00"
      },
      "keywords": ["product management", "agile", "leadership"]
    }
  ],
  "pagination": {
    "page": 0,
    "size": 5,
    "totalElements": 8,
    "totalPages": 2,
    "hasNextPage": true
  }
}
```

#### Get All Jobs (Applicant View)

```
GET /api/v1/jobs/get-all-jobs
```

Get all available jobs with pagination, sorted by most recently updated first. This endpoint is intended for job applicants to browse available job postings and does not require authentication.

**Parameters:**
- `page` (optional): The page number (0-based, defaults to 0)
- `size` (optional): The number of items per page (defaults to 10)

**Request Example:**
```
GET /api/v1/jobs/get-all-jobs?page=0&size=10
```

**Response:**
```json
{
  "jobs": [
    {
      "job": {
        "id": 1,
        "owner": {
          "id": 1,
          "username": "recruiter@example.com",
          "firstName": "Jane",
          "lastName": "Recruiter",
          "isRecruiter": true,
          "companyName": "Acme Inc."
        },
        "description": "We are looking for a software engineer...",
        "title": "Software Engineer",
        "address": "123 Main Street, San Francisco, CA 94105",
        "accessKey": "abc-1234",
        "type": "FULL_TIME",
        "shiftType": "DAY_SHIFT",
        "shiftLengthHours": 8,
        "createdAt": "2025-02-26T10:00:00",
        "updatedAt": "2025-03-04T15:30:00"
      },
      "keywords": ["java", "spring", "api", "microservices"]
    },
    {
      "job": {
        "id": 3,
        "owner": {
          "id": 2,
          "username": "another-recruiter@example.com",
          "firstName": "John",
          "lastName": "Smith",
          "isRecruiter": true,
          "companyName": "Tech Solutions Inc."
        },
        "description": "Senior frontend developer needed for an exciting project...",
        "title": "Senior Frontend Developer",
        "address": "789 Oak Avenue, San Francisco, CA 94105",
        "accessKey": "ghi-9012",
        "type": "FULL_TIME",
        "shiftType": "FLEXIBLE_SHIFT",
        "shiftLengthHours": 8,
        "createdAt": "2025-03-01T09:45:00",
        "updatedAt": "2025-03-04T14:20:00"
      },
      "keywords": ["react", "javascript", "typescript", "css"]
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 15,
    "totalPages": 2,
    "hasNextPage": true
  }
}
```

#### Search Jobs (Semantic Search)

```
GET /api/v1/jobs/search
```

Search for jobs using semantic relevance to a query term. Results are scored based on how closely they match the search term, with matches in job titles weighted more heavily than matches in descriptions or keywords. This endpoint is publicly accessible and does not require authentication.

**Parameters:**
- `query` (required): The search term (e.g., "developer", "java")
- `page` (optional): The page number (0-based, defaults to 0)
- `size` (optional): The number of items per page (defaults to 10)

**Request Example:**
```
GET /api/v1/jobs/search?query=developer&page=0&size=10
```

**Response:**
```json
{
  "jobs": [
    {
      "job": {
        "id": 3,
        "owner": {
          "id": 2,
          "username": "recruiter@example.com",
          "firstName": "John",
          "lastName": "Smith",
          "isRecruiter": true,
          "companyName": "Tech Solutions Inc."
        },
        "description": "Senior frontend developer needed for an exciting project...",
        "title": "Senior Frontend Developer",
        "address": "789 Oak Avenue, San Francisco, CA 94105",
        "accessKey": "ghi-9012",
        "type": "FULL_TIME",
        "shiftType": "FLEXIBLE_SHIFT",
        "shiftLengthHours": 8,
        "createdAt": "2025-03-01T09:45:00",
        "updatedAt": "2025-03-04T14:20:00"
      },
      "keywords": ["javascript", "react", "frontend", "web"],
      "relevanceScore": 0.8
    },
    {
      "job": {
        "id": 5,
        "owner": {
          "id": 1,
          "username": "another-recruiter@example.com",
          "firstName": "Jane",
          "lastName": "Recruiter",
          "isRecruiter": true,
          "companyName": "Acme Inc."
        },
        "description": "Looking for a talented backend developer with Java experience...",
        "title": "Backend Developer",
        "address": "456 Market Street, San Francisco, CA 94105",
        "accessKey": "jkl-3456",
        "type": "FULL_TIME",
        "shiftType": "DAY_SHIFT",
        "shiftLengthHours": 8,
        "createdAt": "2025-02-28T14:15:00",
        "updatedAt": "2025-03-03T10:30:00"
      },
      "keywords": ["java", "spring", "backend", "api"],
      "relevanceScore": 0.7
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 8,
    "totalPages": 1,
    "hasNextPage": false
  }
}
```

Note that each job includes a `relevanceScore` field indicating how closely it matches the search query. Scores range from 0 to 1, with higher values indicating better matches.

#### Get Job Details

```
GET /api/v1/jobs/get-job-details/{accessKey}
```

Get detailed information about a specific job, including associated keywords.

**Parameters:**
- `accessKey`: The unique identifier for the job (e.g., "abc-1234")

**Response (Success - 200 OK):**
```json
{
  "job": {
    "id": 1,
    "owner": {
      "id": 1,
      "username": "recruiter@example.com",
      "firstName": "Jane",
      "lastName": "Recruiter",
      "isRecruiter": true,
      "companyName": "Acme Inc."
    },
    "description": "We are looking for a software engineer...",
    "title": "Software Engineer",
    "address": "123 Main Street, San Francisco, CA 94105",
    "accessKey": "abc-1234",
    "type": "FULL_TIME",
    "shiftType": "DAY_SHIFT",
    "shiftLengthHours": 8,
    "createdAt": "2025-02-26T10:00:00",
    "updatedAt": "2025-02-26T10:00:00"
  },
  "keywords": ["java", "spring", "api", "microservices"]
}
```

**Response (Not Found - 404):**
```json
{
  "message": "Cannot find job with access key: invalid-key."
}
```

#### Delete Job

```
DELETE /api/v1/jobs/delete-job/{accessKey}
```

Delete a specific job.

**Parameters:**
- `accessKey`: The unique identifier for the job (e.g., "abc-1234")

**Response:**
- `204 No Content`: Job successfully deleted
- `404 Not Found`: If the job with the specified access key doesn't exist
- `403 Forbidden`: If the authenticated user doesn't own the job
- `401 Unauthorized`: If the request is not authenticated

#### Edit Job

```
PUT /api/v1/jobs/edit-job/{accessKey}
```

Update an existing job posting.

**Parameters:**
- `accessKey`: The unique identifier for the job (e.g., "abc-1234")

**Request Body:**
```json
{
  "title": "Updated Software Engineer",        // required
  "description": "We are looking for...",     // required
  "address": "123 Main Street, SF, CA",       // required
  "type": "FULL_TIME",                        // required
  "shiftType": "DAY_SHIFT",                   // optional
  "shiftLengthHours": 8,                       // optional
  "locationType": "ON_SITE",                  // required
  "keywords": ["java", "spring", "api"]      // optional
}
```

**Response (Success - 200 OK):**
```json
{
  "job": {
    "id": 1,
    "owner": {
      "id": 1,
      "username": "recruiter@example.com",
      "firstName": "Jane",
      "lastName": "Recruiter",
      "isRecruiter": true
    },
    "description": "We are looking for an experienced software engineer...",
    "title": "Updated Software Engineer",
    "address": "123 Main Street, San Francisco, CA 94105",
    "accessKey": "abc-1234",
    "type": "FULL_TIME",
    "shiftType": "DAY_SHIFT",
    "shiftLengthHours": 8,
    "createdAt": "2025-02-26T10:00:00",
    "updatedAt": "2025-03-04T15:30:00"
  },
  "keywords": ["java", "spring", "api", "microservices", "cloud"]
}
```

**Response (Error):**
- `404 Not Found`: If the job with the specified access key doesn't exist
- `403 Forbidden`: If the authenticated user doesn't own the job
- `400 Bad Request`: If the request body is invalid
- `401 Unauthorized`: If the request is not authenticated

#### Get Job Metadata

```
GET /api/v1/jobs/job-metadata
```

Get all available job types and shift types.

**Response:**
```json
{
  "jobTypes": ["FULL_TIME", "PART_TIME", "INTERNSHIP", "CONTRACT"],
  "shiftTypes": ["DAY_SHIFT", "NIGHT_SHIFT", "ROTATING_SHIFT", "FLEXIBLE_SHIFT"],
  "locationTypes": ["REMOTE", "HYBRID", "ON_SITE"]
}
```

### Resume Processing

#### Upload Resume

```
POST /api/v1/resume/upload
```

Upload a resume file with keywords for analysis.

**Request Body:**
Multipart form data:
- `resume`: PDF file (required)
- `keywords`: List of keywords (required, at least one keyword)

**Response:**
```json
{
  "hash": "uniqueIdentifier"
}
```

#### Check Processing Status

```
GET /api/v1/resume/status/{hash}
```

Check the status of resume processing.

**Parameters:**
- `hash`: The unique identifier returned from upload

**Response:**
```json
{
  "result": "PROCESSING" | "COMPLETED" | "FAILED"
}
```

#### Get Similarity Score

```
GET /api/v1/resume/score/{hash}
```

Get the similarity analysis results for a processed resume.

**Parameters:**
- `hash`: The unique identifier returned from upload

**Response:**
```json
{
  "hash": "uniqueIdentifier",
  "result": { /* Analysis results */ }
}
```

### Interviewer

#### Request WebSocket Connection

```
GET /api/v1/interviewer/request-ws-connection
```

Request a ticket for establishing a WebSocket connection for interviewing.

**Response:**
```json
{
  "ticket": "uniqueTicketString"
}
```

#### Check WebSocket Connection

```
GET /api/v1/interviewer/check-ws-connection?ticket={ticket}
```

Check if a WebSocket connection has been initiated.

**Parameters:**
- `ticket`: The ticket string from the request endpoint

**Response:**
```json
{
  "status": "initiated" | "not-initiated" | "unauthorized"
}
```

## WebSocket Protocol

The WebSocket connection uses a binary protocol for real-time interviewing. The message format is:

```
[4 bytes messageHeader] + [4 bytes message length] + [JSON message bytes] + [binary storage bytes]
```

Message types:
- `0`: Heartbeat (keep-alive)
- `1`: Client audio send
- `2`: AI audio response
- `3`: Client text send
- `4`: Transcription text
- `5`: Client authentication request

## API Access Information

### Authentication Requirements

Most API endpoints require authentication using the JWT token provided during login. However, the following endpoints are publicly accessible without authentication:

- All authentication endpoints (`/api/v1/auth/**`)
- Job listing for applicants (`/api/v1/jobs/get-all-jobs`)
- Job search endpoint (`/api/v1/jobs/search`)
- Job details (`/api/v1/jobs/get-job-details/{accessKey}`)
- Job metadata (`/api/v1/jobs/job-metadata`)
- Debug endpoints (`/debug/**`)
- WebSocket endpoints (`/ws/**`)

All other endpoints require a valid JWT token to be included in the request cookies.

## Model Information

### Job Types
- `FULL_TIME`: Full-time employment
- `PART_TIME`: Part-time employment
- `INTERNSHIP`: Internship position
- `CONTRACT`: Contract-based work

### Shift Types
- `DAY_SHIFT`: Regular daytime hours
- `NIGHT_SHIFT`: Overnight working hours
- `ROTATING_SHIFT`: Schedule that changes regularly
- `FLEXIBLE_SHIFT`: Flexible working hours

### Location Types
- `REMOTE`: Work performed entirely from home or location of employee's choice
- `HYBRID`: Combination of remote and in-office work
- `ON_SITE`: Work performed entirely at the employer's location

### User Types
- `Applicant (isApplicant=true)`: A job seeker who can upload resumes and apply to jobs
- `Recruiter (isApplicant=false)`: A user who can create and manage job postings

## Field Requirements

### Authentication

#### Sign Up - Applicants
- `email`: Required - Must be a unique email address
- `password`: Required - User's password
- `firstName`: Required - User's first name
- `lastName`: Required - User's last name
- `isApplicant`: Required - Must be set to `true` for applicants

#### Sign Up - Recruiters
- `email`: Required - Must be a unique email address
- `password`: Required - User's password
- `firstName`: Required - User's first name
- `lastName`: Required - User's last name
- `isApplicant`: Required - Must be set to `false` for recruiters
- `companyName`: Required for recruiters - Company or organization name

#### Login
- `email`: Required - Registered email address
- `password`: Required - User's password

### Jobs

#### Create/Edit Job
- `title`: Required - Job title
- `description`: Required - Detailed job description
- `address`: Required - Physical location of the job
- `type`: Required - One of: FULL_TIME, PART_TIME, INTERNSHIP, CONTRACT
- `locationType`: Required - One of: REMOTE, HYBRID, ON_SITE
- `shiftType`: Optional - One of: DAY_SHIFT, NIGHT_SHIFT, ROTATING_SHIFT, FLEXIBLE_SHIFT
- `shiftLengthHours`: Optional - Positive integer representing shift length in hours
- `keywords`: Optional - Array of strings representing skills or keywords

### Resume Processing

#### Upload Resume
- `resume`: Required - PDF file of the resume
- `keywords`: Required - At least one keyword for comparison
