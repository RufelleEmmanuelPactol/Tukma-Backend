# Questions API

This document details the question management endpoints for the Tukma application.

## Endpoints

### Get All Questions for a Job

```
GET /api/v1/jobs/questions/{accessKey}
```

Retrieve all questions associated with a specific job posting.

**Parameters:**
- `accessKey`: The unique identifier for the job (e.g., "abc-1234")

**Response (Success - 200 OK):**
```json
[
  {
    "id": 1,
    "questionText": "Describe your experience with Java programming.",
    "type": "TECHNICAL",
    "createdAt": "2025-03-01T10:00:00",
    "updatedAt": "2025-03-01T10:00:00"
  },
  {
    "id": 2,
    "questionText": "Tell me about a time when you had to solve a complex problem.",
    "type": "BEHAVIORAL",
    "createdAt": "2025-03-01T10:15:00",
    "updatedAt": "2025-03-01T10:15:00"
  }
]
```

**Response (Error):**
```json
{
  "message": "Job not found with access key: invalid-key"
}
```

### Get Question Count

```
GET /api/v1/jobs/questions/{accessKey}/count
```

Get the total number of questions for a specific job.

**Parameters:**
- `accessKey`: The unique identifier for the job

**Response (Success - 200 OK):**
```json
{
  "count": 5
}
```

**Response (Error):**
```json
{
  "message": "Job not found with access key: invalid-key"
}
```

### Add a Question

```
POST /api/v1/jobs/questions/{accessKey}
```

Add a new question to a job posting.

**Parameters:**
- `accessKey`: The unique identifier for the job

**Request Body:**
```json
{
  "questionText": "Explain the differences between REST and GraphQL.",
  "type": "TECHNICAL"
}
```

**Response (Success - 201 Created):**
```json
{
  "id": 3,
  "questionText": "Explain the differences between REST and GraphQL.",
  "type": "TECHNICAL",
  "createdAt": "2025-03-08T14:30:00",
  "updatedAt": "2025-03-08T14:30:00"
}
```

**Response (Error):**
- `400 Bad Request`: If required fields are missing or invalid
- `401 Unauthorized`: If the request is not authenticated
- `403 Forbidden`: If the user is not authorized to modify this job
- `404 Not Found`: If the job with the specified access key doesn't exist

### Add Multiple Questions (Batch)

```
POST /api/v1/jobs/questions/{accessKey}/batch
```

Add multiple questions to a job posting in a single request.

**Parameters:**
- `accessKey`: The unique identifier for the job

**Request Body:**
```json
{
  "questions": [
    "What experience do you have with microservices?",
    "Describe your approach to testing.",
    "How do you handle code reviews?"
  ],
  "type": "TECHNICAL"
}
```

**Response (Success - 201 Created):**
```json
[
  {
    "id": 4,
    "questionText": "What experience do you have with microservices?",
    "type": "TECHNICAL",
    "createdAt": "2025-03-08T14:35:00",
    "updatedAt": "2025-03-08T14:35:00"
  },
  {
    "id": 5,
    "questionText": "Describe your approach to testing.",
    "type": "TECHNICAL",
    "createdAt": "2025-03-08T14:35:00",
    "updatedAt": "2025-03-08T14:35:00"
  },
  {
    "id": 6,
    "questionText": "How do you handle code reviews?",
    "type": "TECHNICAL", 
    "createdAt": "2025-03-08T14:35:00",
    "updatedAt": "2025-03-08T14:35:00"
  }
]
```

**Response (Error):**
- `400 Bad Request`: If required fields are missing or invalid
- `401 Unauthorized`: If the request is not authenticated
- `403 Forbidden`: If the user is not authorized to modify this job
- `404 Not Found`: If the job with the specified access key doesn't exist

### Update a Question

```
PUT /api/v1/jobs/questions/{accessKey}/{questionId}
```

Update an existing question.

**Parameters:**
- `accessKey`: The unique identifier for the job
- `questionId`: The ID of the question to update

**Request Body:**
```json
{
  "questionText": "Updated question text",
  "type": "BEHAVIORAL"
}
```

**Response (Success - 200 OK):**
```json
{
  "id": 3,
  "questionText": "Updated question text",
  "type": "BEHAVIORAL",
  "createdAt": "2025-03-08T14:30:00",
  "updatedAt": "2025-03-08T15:00:00"
}
```

**Response (Error):**
- `400 Bad Request`: If required fields are missing or invalid
- `401 Unauthorized`: If the request is not authenticated
- `403 Forbidden`: If the user is not authorized to modify this job
- `404 Not Found`: If the job or question doesn't exist

### Delete a Question

```
DELETE /api/v1/jobs/questions/{accessKey}/{questionId}
```

Delete a specific question.

**Parameters:**
- `accessKey`: The unique identifier for the job
- `questionId`: The ID of the question to delete

**Response:**
- `204 No Content`: Question successfully deleted
- `401 Unauthorized`: If the request is not authenticated
- `403 Forbidden`: If the user is not authorized to modify this job
- `404 Not Found`: If the job or question doesn't exist

### Delete All Questions for a Job

```
DELETE /api/v1/jobs/questions/{accessKey}/all
```

Delete all questions associated with a job.

**Parameters:**
- `accessKey`: The unique identifier for the job

**Response:**
- `204 No Content`: All questions successfully deleted
- `401 Unauthorized`: If the request is not authenticated
- `403 Forbidden`: If the user is not authorized to modify this job
- `404 Not Found`: If the job doesn't exist

## Question Types

Questions in the Tukma application are categorized into two types:

- `TECHNICAL`: Questions related to technical skills, knowledge, and expertise 
- `BEHAVIORAL`: Questions related to soft skills, past experiences, and problem-solving approaches

All question endpoints require authentication, and only the job owner (recruiter) can add, modify, or delete questions for a job.

## Field Requirements

### Add/Update Question
- `questionText`: Required - The text of the interview question
- `type`: Required - Must be one of: TECHNICAL, BEHAVIORAL

### Batch Add Questions
- `questions`: Required - Array of question texts (strings)
- `type`: Required - Must be one of: TECHNICAL, BEHAVIORAL (applied to all questions in the batch)
