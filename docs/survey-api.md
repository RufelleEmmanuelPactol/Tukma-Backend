# Survey API

This document details the survey question and answer management endpoints for the Tukma application.

## Question Endpoints

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

## Answer Endpoints

### Get All Answers (Admin)

```
GET /api/v1/survey/answers
```

Retrieve all survey answers across all users. This endpoint is intended for administrators.

**Response (Success - 200 OK):**
```json
[
  {
    "id": 1,
    "question": {
      "id": 1,
      "questionText": "How would you rate your experience with the application process?",
      "createdAt": "2025-04-01T10:00:00",
      "updatedAt": "2025-04-01T10:00:00"
    },
    "user": {
      "id": 3,
      "username": "applicant@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "isRecruiter": false
    },
    "score": 4,
    "createdAt": "2025-04-05T14:30:00",
    "updatedAt": "2025-04-05T14:30:00"
  },
  {
    "id": 2,
    "question": {
      "id": 2,
      "questionText": "Did you find the job description clear and informative?",
      "createdAt": "2025-03-28T15:30:00",
      "updatedAt": "2025-03-28T15:30:00"
    },
    "user": {
      "id": 4,
      "username": "user2@example.com",
      "firstName": "Jane",
      "lastName": "Smith",
      "isRecruiter": false
    },
    "score": 5,
    "createdAt": "2025-04-06T09:15:00",
    "updatedAt": "2025-04-06T09:15:00"
  }
]
```

### Get Current User's Answers

```
GET /api/v1/survey/answers/me
```

Retrieve all survey answers submitted by the currently authenticated user.

**Response (Success - 200 OK):**
```json
[
  {
    "id": 1,
    "question": {
      "id": 1,
      "questionText": "How would you rate your experience with the application process?",
      "createdAt": "2025-04-01T10:00:00",
      "updatedAt": "2025-04-01T10:00:00"
    },
    "user": {
      "id": 3,
      "username": "applicant@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "isRecruiter": false
    },
    "score": 4,
    "createdAt": "2025-04-05T14:30:00",
    "updatedAt": "2025-04-05T14:30:00"
  }
]
```

**Response (Error - 401 Unauthorized):**
```json
{
  "error": "You must be logged in to view your answers"
}
```

### Get Answers for a Specific Question

```
GET /api/v1/survey/answers/question/{questionId}
```

Retrieve all answers for a specific question, along with statistics.

**Parameters:**
- `questionId`: The unique identifier for the question

**Response (Success - 200 OK):**
```json
{
  "answers": [
    {
      "id": 1,
      "question": {
        "id": 1,
        "questionText": "How would you rate your experience with the application process?",
        "createdAt": "2025-04-01T10:00:00",
        "updatedAt": "2025-04-01T10:00:00"
      },
      "user": {
        "id": 3,
        "username": "applicant@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "isRecruiter": false
      },
      "score": 4,
      "createdAt": "2025-04-05T14:30:00",
      "updatedAt": "2025-04-05T14:30:00"
    }
  ],
  "count": 15,
  "averageScore": 4.2
}
```

### Submit an Answer

```
POST /api/v1/survey/answers
```

Submit a new answer or update an existing one for a question.

**Request Body:**
```json
{
  "questionId": 1,
  "score": 4
}
```

**Response (Created - 201 OK or Updated - 200 OK):**
```json
{
  "id": 1,
  "question": {
    "id": 1,
    "questionText": "How would you rate your experience with the application process?",
    "createdAt": "2025-04-01T10:00:00",
    "updatedAt": "2025-04-01T10:00:00"
  },
  "user": {
    "id": 3,
    "username": "applicant@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "isRecruiter": false
  },
  "score": 4,
  "createdAt": "2025-04-05T14:30:00",
  "updatedAt": "2025-04-05T14:30:00"
}
```

**Response (Error - 400 Bad Request):**
```json
{
  "error": "Score must be between 1 and 5"
}
```

**Response (Error - 401 Unauthorized):**
```json
{
  "error": "You must be logged in to submit answers"
}
```

### Submit Multiple Answers (Complete Survey)

```
POST /api/v1/survey/answers/submit-survey
```

Submit multiple answers at once (for a complete survey).

**Request Body:**
```json
{
  "answers": [
    {
      "questionId": 1,
      "score": 4
    },
    {
      "questionId": 2,
      "score": 5
    },
    {
      "questionId": 3,
      "score": 3
    }
  ]
}
```

**Response (Success - 200 OK):**
```json
{
  "message": "Survey submitted successfully",
  "answers": [
    {
      "id": 1,
      "question": {
        "id": 1,
        "questionText": "How would you rate your experience with the application process?",
        "createdAt": "2025-04-01T10:00:00",
        "updatedAt": "2025-04-01T10:00:00"
      },
      "user": {
        "id": 3,
        "username": "applicant@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "isRecruiter": false
      },
      "score": 4,
      "createdAt": "2025-04-05T14:30:00",
      "updatedAt": "2025-04-05T14:30:00"
    },
    {
      "id": 2,
      "question": {
        "id": 2,
        "questionText": "Did you find the job description clear and informative?",
        "createdAt": "2025-03-28T15:30:00",
        "updatedAt": "2025-03-28T15:30:00"
      },
      "user": {
        "id": 3,
        "username": "applicant@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "isRecruiter": false
      },
      "score": 5,
      "createdAt": "2025-04-05T14:30:00",
      "updatedAt": "2025-04-05T14:30:00"
    },
    {
      "id": 3,
      "question": {
        "id": 3,
        "questionText": "Would you recommend this job platform to others?",
        "createdAt": "2025-04-01T10:00:00",
        "updatedAt": "2025-04-01T10:00:00"
      },
      "user": {
        "id": 3,
        "username": "applicant@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "isRecruiter": false
      },
      "score": 3,
      "createdAt": "2025-04-05T14:30:00",
      "updatedAt": "2025-04-05T14:30:00"
    }
  ]
}
```

**Response (Error - 400 Bad Request):**
```json
{
  "error": "Each answer must have questionId and score"
}
```

### Update an Answer

```
PUT /api/v1/survey/answers/{id}
```

Update an existing answer.

**Parameters:**
- `id`: The unique identifier for the answer to update

**Request Body:**
```json
{
  "score": 5
}
```

**Response (Success - 200 OK):**
```json
{
  "id": 1,
  "question": {
    "id": 1,
    "questionText": "How would you rate your experience with the application process?",
    "createdAt": "2025-04-01T10:00:00",
    "updatedAt": "2025-04-01T10:00:00"
  },
  "user": {
    "id": 3,
    "username": "applicant@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "isRecruiter": false
  },
  "score": 5,
  "createdAt": "2025-04-05T14:30:00",
  "updatedAt": "2025-04-08T16:45:00"
}
```

**Response (Error - 403 Forbidden):**
```json
{
  "error": "You can only update your own answers"
}
```

### Delete an Answer

```
DELETE /api/v1/survey/answers/{id}
```

Delete a specific answer.

**Parameters:**
- `id`: The unique identifier for the answer to delete

**Response:**
- `204 No Content`: Answer successfully deleted
- `404 Not Found`: If the answer with the specified ID doesn't exist
- `403 Forbidden`: If the user is not the owner of the answer

### Get Survey Statistics

```
GET /api/v1/survey/answers/statistics
```

Get statistics for all survey questions.

**Response (Success - 200 OK):**
```json
{
  "statistics": [
    {
      "question": {
        "id": 1,
        "questionText": "How would you rate your experience with the application process?",
        "createdAt": "2025-04-01T10:00:00",
        "updatedAt": "2025-04-01T10:00:00"
      },
      "averageScore": 4.2,
      "responseCount": 15,
      "scoreDistribution1": 0,
      "scoreDistribution2": 1,
      "scoreDistribution3": 2,
      "scoreDistribution4": 8,
      "scoreDistribution5": 4
    },
    {
      "question": {
        "id": 2,
        "questionText": "Did you find the job description clear and informative?",
        "createdAt": "2025-03-28T15:30:00",
        "updatedAt": "2025-03-28T15:30:00"
      },
      "averageScore": 3.8,
      "responseCount": 12,
      "scoreDistribution1": 0,
      "scoreDistribution2": 2,
      "scoreDistribution3": 3,
      "scoreDistribution4": 4,
      "scoreDistribution5": 3
    }
  ],
  "totalQuestions": 2
}
```

### Get System Usability Scale (SUS) Score

```
GET /api/v1/survey/answers/sus-score
```

Calculate the System Usability Scale (SUS) score for the current user.

**Response (Success - 200 OK):**
```json
{
  "susScore": 72.5,
  "interpretation": "Good",
  "answeredQuestions": 10,
  "possibleMax": 100.0
}
```

**Response (Error - 401 Unauthorized):**
```json
{
  "error": "You must be logged in to calculate SUS score"
}
```

### Get Overall SUS Statistics

```
GET /api/v1/survey/answers/overall-sus-statistics
```

Get overall SUS statistics across all users (admin only).

**Response (Success - 200 OK):**
```json
{
  "averageSusScore": 68.7,
  "totalRespondents": 45,
  "overallInterpretation": "OK",
  "scoreDistribution": {
    "excellent": 5,
    "good": 18,
    "ok": 15,
    "poor": 5,
    "awful": 2
  },
  "userScores": [
    {
      "userId": 3,
      "username": "applicant@example.com",
      "susScore": 72.5,
      "interpretation": "Good"
    },
    {
      "userId": 4,
      "username": "user2@example.com",
      "susScore": 67.5,
      "interpretation": "OK"
    }
  ]
}
```

### Check Survey Completion Status

```
GET /api/v1/survey/answers/check-survey-completion
```

Check if the current user has completed the SUS survey (answered at least 10 unique questions).

**Response (Success - 200 OK):**
```json
{
  "isComplete": true,
  "answeredQuestions": 10,
  "requiredQuestions": 10,
  "remainingQuestions": 0
}
```

Or for an incomplete survey:

```json
{
  "isComplete": false,
  "answeredQuestions": 7,
  "requiredQuestions": 10,
  "remainingQuestions": 3
}
```

**Response (Error - 401 Unauthorized):**
```json
{
  "error": "You must be logged in to check survey completion"
}
```

## Field Requirements

### Create/Update Question
- `questionText`: Required - The text of the survey question

### Submit/Update Answer
- `questionId`: Required (for POST) - The ID of the question being answered
- `score`: Required - Integer value between 1 and 5 (5-point Likert scale)

## Usage Context

Survey questions and answers can be used in various parts of the application:

1. **Post-Application Surveys**: Gather feedback from applicants after they submit a job application
2. **Post-Interview Surveys**: Collect feedback about the interview experience
3. **General Platform Feedback**: Understand user satisfaction with the overall platform

Survey questions are designed to be flexible and can be used across multiple contexts within the application. The System Usability Scale (SUS) provides a standardized measure of perceived usability with scores ranging from 0-100.
