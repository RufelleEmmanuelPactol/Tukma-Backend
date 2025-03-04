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
  "email": "applicant@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe",
  "isApplicant": true
}
```

**Request Body (for Recruiters):**
```json
{
  "email": "recruiter@example.com",
  "password": "password123",
  "firstName": "Jane",
  "lastName": "Smith",
  "isApplicant": false,
  "companyName": "Acme Inc."
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
  "email": "user@example.com",
  "password": "password123"
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
  "title": "Software Engineer",
  "description": "We are looking for a software engineer...",
  "address": "123 Main Street, San Francisco, CA 94105",
  "type": "FULL_TIME",
  "shiftType": "DAY_SHIFT",
  "shiftLengthHours": 8,
  "keywords": ["java", "spring", "api", "microservices"]
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

#### Get All Jobs

```
GET /api/v1/jobs/get-jobs
```

Get all jobs created by the authenticated user, including associated keywords.

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
  "title": "Updated Software Engineer",
  "description": "We are looking for an experienced software engineer...",
  "address": "123 Main Street, San Francisco, CA 94105",
  "type": "FULL_TIME",
  "shiftType": "DAY_SHIFT",
  "shiftLengthHours": 8,
  "keywords": ["java", "spring", "api", "microservices", "cloud"]
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
  "shiftTypes": ["DAY_SHIFT", "NIGHT_SHIFT", "ROTATING_SHIFT", "FLEXIBLE_SHIFT"]
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
- `resume`: PDF file
- `keywords`: List of keywords

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

### User Types
- `Applicant (isApplicant=true)`: A job seeker who can upload resumes and apply to jobs
- `Recruiter (isApplicant=false)`: A user who can create and manage job postings
