# Survey API

This document details the survey question management endpoints for the Tukma application.

## Endpoints

### Get All Questions

```
GET /api/v1/survey/questions
```

Retrieve all survey questions, ordered by creation date (newest first).

**Response (Success - 200 OK):**
```json
[
  {
    "id": 1,
    "questionText": "How would you rate your experience with the application process?",
    "createdAt": "2025-04-01T10:00:00",
    "updatedAt": "2025-04-01T10:00:00"
  },
  {
    "id": 2,
    "questionText": "Did you find the job description clear and informative?",
    "createdAt": "2025-03-28T15:30:00",
    "updatedAt": "2025-03-28T15:30:00"
  }
]
```

### Get Question by ID

```
GET /api/v1/survey/questions/{id}
```

Retrieve a specific survey question by its ID.

**Parameters:**
- `id`: The unique identifier for the question

**Response (Success - 200 OK):**
```json
{
  "id": 1,
  "questionText": "How would you rate your experience with the application process?",
  "createdAt": "2025-04-01T10:00:00",
  "updatedAt": "2025-04-01T10:00:00"
}
```

**Response (Not Found - 404):**
Empty response body.

### Create Question

```
POST /api/v1/survey/questions
```

Create a new survey question.

**Request Body:**
```json
{
  "questionText": "Would you recommend this job platform to others?"
}
```

**Response (Success - 201 Created):**
```json
{
  "id": 3,
  "questionText": "Would you recommend this job platform to others?",
  "createdAt": "2025-04-08T14:30:00",
  "updatedAt": "2025-04-08T14:30:00"
}
```

**Response (Error - 400 Bad Request):**
```json
{
  "error": "Question text is required"
}
```

### Update Question

```
PUT /api/v1/survey/questions/{id}
```

Update an existing survey question.

**Parameters:**
- `id`: The unique identifier for the question to update

**Request Body:**
```json
{
  "questionText": "Updated question text: How satisfied were you with the application process?"
}
```

**Response (Success - 200 OK):**
```json
{
  "id": 1,
  "questionText": "Updated question text: How satisfied were you with the application process?",
  "createdAt": "2025-04-01T10:00:00",
  "updatedAt": "2025-04-08T15:45:00"
}
```

**Response (Not Found - 404):**
Empty response body.

**Response (Error - 400 Bad Request):**
```json
{
  "error": "Question text is required"
}
```

### Delete Question

```
DELETE /api/v1/survey/questions/{id}
```

Delete a specific survey question.

**Parameters:**
- `id`: The unique identifier for the question to delete

**Response:**
- `204 No Content`: Question successfully deleted
- `404 Not Found`: If the question with the specified ID doesn't exist

### Search Questions

```
GET /api/v1/survey/questions/search?term={term}
```

Search for questions containing the specified search term (case-insensitive).

**Parameters:**
- `term`: The text to search for within question text

**Response (Success - 200 OK):**
```json
[
  {
    "id": 1,
    "questionText": "How would you rate your experience with the application process?",
    "createdAt": "2025-04-01T10:00:00",
    "updatedAt": "2025-04-01T10:00:00"
  },
  {
    "id": 4,
    "questionText": "Was your experience with the interview comfortable?",
    "createdAt": "2025-03-25T09:15:00",
    "updatedAt": "2025-03-25T09:15:00"
  }
]
```

## Field Requirements

### Create/Update Question
- `questionText`: Required - The text of the survey question

## Usage Context

Survey questions can be used in various parts of the application:

1. **Post-Application Surveys**: Gather feedback from applicants after they submit a job application
2. **Post-Interview Surveys**: Collect feedback about the interview experience
3. **General Platform Feedback**: Understand user satisfaction with the overall platform

Survey questions are designed to be flexible and can be used across multiple contexts within the application.
