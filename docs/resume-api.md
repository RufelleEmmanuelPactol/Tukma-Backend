# Resume API

This document details the resume processing endpoints for the Tukma application.

## Endpoints

### Upload Resume (General)

```
POST /api/v1/resume/upload
```

Upload a resume file with keywords for analysis. This endpoint is used for general resume analysis without associating it with a specific job.

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

### Upload Resume for Job Application

```
POST /api/v1/resume/upload-for-job/{accessKey}
```

Upload a resume file for a specific job application. This endpoint associates the resume with the job and the current user.

**Parameters:**
- `accessKey`: The access key of the job being applied for

**Request Body:**
Multipart form data:
- `resume`: PDF file (required)
- `keywords`: List of keywords (required, at least one keyword)

**Response (Success - 200 OK):**
```json
{
  "hash": "uniqueIdentifier"
}
```

**Response (Error):**
```json
{
  "error": "Job not found with access key: invalid-key"
}
```

### Check Processing Status

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

### Get Similarity Score

```
GET /api/v1/resume/score/{hash}
```

Get the similarity analysis results for a processed resume. If the resume is associated with a job, this endpoint will also update the job association in the database.

**Parameters:**
- `hash`: The unique identifier returned from upload

**Response:**
```json
{
  "hash": "uniqueIdentifier",
  "result": {
    "javascript": {
      "similarity_score": 0.48658517708123633,
      "best_matching_ngram": "in Laravel, JavaScript,"
    },
    "frontend": {
      "similarity_score": 0.6945115131278724,
      "best_matching_ngram": "the frontend and"
    },
    "software engineer": {
      "similarity_score": 0.6592739827568643,
      "best_matching_ngram": "Information Technology Engineers"
    }
  }
}
```

### Get Resume Data

```
GET /api/v1/resume/data/{hash}
```

Get stored resume data from the database with the parsed results.

**Parameters:**
- `hash`: The resume hash identifier

**Response (Success - 200 OK):**
```json
{
  "resume": {
    "id": 1,
    "resumeHash": "uniqueIdentifier",
    "results": "...", // Raw results JSON
    "job": {
      "id": 5,
      "title": "Software Engineer",
      "...": "..."
    },
    "owner": {
      "id": 3,
      "username": "applicant@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "isRecruiter": false
    }
  },
  "parsedResults": {
    "javascript": {
      "similarity_score": 0.48658517708123633,
      "best_matching_ngram": "in Laravel, JavaScript,"
    },
    "frontend": {
      "similarity_score": 0.6945115131278724,
      "best_matching_ngram": "the frontend and"
    }
  }
}
```

**Response (Not Found - 404):** Empty response

### Get Resumes by Job

```
GET /api/v1/resume/job/{accessKey}
```

Get all resumes submitted for a specific job. This endpoint is only accessible to the job owner (recruiter).

**Parameters:**
- `accessKey`: The job access key

**Response (Success - 200 OK):**
```json
{
  "job": {
    "id": 5,
    "title": "Software Engineer",
    "accessKey": "abc-1234",
    "...": "..."
  },
  "resumes": [
    {
      "resume": {
        "id": 1,
        "resumeHash": "uniqueIdentifier1",
        "...": "..."
      },
      "parsedResults": {
        "javascript": {
          "similarity_score": 0.7,
          "best_matching_ngram": "JavaScript developer with 5 years"
        },
        "...": "..."
      }
    },
    {
      "resume": {
        "id": 2,
        "resumeHash": "uniqueIdentifier2",
        "...": "..."
      },
      "parsedResults": {
        "...": "..."
      }
    }
  ]
}
```

**Response (Error):**
- `404 Not Found`: If the job with the specified access key doesn't exist
- `403 Forbidden`: If the authenticated user is not the owner of the job
- `401 Unauthorized`: If the request is not authenticated

### Get My Resumes

```
GET /api/v1/resume/my-resumes
```

Get all resumes submitted by the currently authenticated applicant user.

**Response (Success - 200 OK):**
```json
{
  "resumes": [
    {
      "resume": {
        "id": 1,
        "resumeHash": "uniqueIdentifier1",
        "...": "..."
      },
      "parsedResults": {
        "javascript": {
          "similarity_score": 0.7,
          "best_matching_ngram": "JavaScript developer with 5 years"
        },
        "...": "..."
      },
      "job": {
        "id": 5,
        "title": "Software Engineer",
        "...": "..."
      }
    },
    {
      "resume": {
        "id": 2,
        "resumeHash": "uniqueIdentifier2",
        "...": "..."
      },
      "parsedResults": {
        "...": "..."
      },
      "job": {
        "id": 7,
        "title": "Frontend Developer",
        "...": "..."
      }
    }
  ]
}
```

**Response (Error):**
- `403 Forbidden`: If the authenticated user is a recruiter (not an applicant)
- `401 Unauthorized`: If the request is not authenticated

### Get My Application for Job

```
GET /api/v1/resume/my-application/{accessKey}
```

Get the resume submitted by the current applicant user for a specific job.

**Parameters:**
- `accessKey`: The access key of the job

**Response (Success - 200 OK):**
```json
{
  "resume": {
    "id": 1,
    "resumeHash": "uniqueIdentifier",
    "...": "..."
  },
  "parsedResults": {
    "javascript": {
      "similarity_score": 0.7,
      "best_matching_ngram": "JavaScript developer with 5 years"
    },
    "...": "..."
  },
  "job": {
    "id": 5,
    "title": "Software Engineer",
    "accessKey": "abc-1234",
    "...": "..."
  }
}
```

**Response (Error):**
- `404 Not Found`: If the job doesn't exist or no application found
- `403 Forbidden`: If the authenticated user is a recruiter (not an applicant)
- `401 Unauthorized`: If the request is not authenticated

### Get Applicant Resume for Job

```
GET /api/v1/resume/job/{accessKey}/applicant/{applicantId}
```

Get a specific applicant's resume for a job. This endpoint is only accessible to recruiters who own the job.

**Parameters:**
- `accessKey`: The access key of the job
- `applicantId`: The ID of the applicant

**Response (Success - 200 OK):**
```json
{
  "resume": {
    "id": 1,
    "resumeHash": "uniqueIdentifier",
    "...": "..."
  },
  "parsedResults": {
    "javascript": {
      "similarity_score": 0.7,
      "best_matching_ngram": "JavaScript developer with 5 years"
    },
    "...": "..."
  },
  "applicant": {
    "id": 3,
    "username": "applicant@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "isRecruiter": false
  },
  "job": {
    "id": 5,
    "title": "Software Engineer",
    "accessKey": "abc-1234",
    "...": "..."
  }
}
```

**Response (Error):**
- `404 Not Found`: If the job doesn't exist or no application found
- `403 Forbidden`: If the authenticated user is not the owner of the job or is not a recruiter
- `401 Unauthorized`: If the request is not authenticated

## Internal Processing

The Resume API utilizes the following internal processing flow:

1. Resume uploaded via the API
2. Resume sent to an external microservice for analysis
3. Analysis results stored in the database
4. Results parsed and formatted for frontend presentation

The similarity scoring system compares resume text against provided keywords using semantic analysis, with scores ranging from 0 to 1, where higher scores indicate better matches.

## Field Requirements

### Upload Resume
- `resume`: Required - PDF file of the resume
- `keywords`: Required - At least one keyword for comparison
