# Jobs API

This document details the job management endpoints for the Tukma application.

## Endpoints

### Create Job

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

### Get All Jobs (Deprecated)

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
  /* Additional jobs... */
]
```

### Get Paginated Jobs (Recruiter View)

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
    /* Additional jobs... */
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

### Get All Jobs (Applicant View)

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
    /* Additional jobs... */
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

### Search Jobs (Semantic Search)

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
    /* Additional jobs... */
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

### Get Job Details

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

### Delete Job

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

### Edit Job

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

### Get Job Metadata

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

## Field Requirements

### Create/Edit Job
- `title`: Required - Job title
- `description`: Required - Detailed job description
- `address`: Required - Physical location of the job
- `type`: Required - One of: FULL_TIME, PART_TIME, INTERNSHIP, CONTRACT
- `locationType`: Required - One of: REMOTE, HYBRID, ON_SITE
- `shiftType`: Optional - One of: DAY_SHIFT, NIGHT_SHIFT, ROTATING_SHIFT, FLEXIBLE_SHIFT
- `shiftLengthHours`: Optional - Positive integer representing shift length in hours
- `keywords`: Optional - Array of strings representing skills or keywords
