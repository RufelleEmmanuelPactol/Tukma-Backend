# Interviewer API

This document details the interviewer endpoints and WebSocket protocol for the Tukma application.

## Scoring Scales

The Tukma API uses the following scoring scales:

- **Technical Skills**: Scores range from 0-100, where 0 is poor and 100 is excellent
- **Communication Skills**: Scores range from 1-10, where 1 is poor and 10 is excellent

## REST Endpoints

### Request WebSocket Connection

```
GET /api/v1/interviewer/request-ws-connection
```

Request a ticket for establishing a WebSocket connection for interviewing.

**Response:**

```json
{
  "ticket": "2025-03-08T14:30:15.123Z-987654321"
}
```

### Check WebSocket Connection

```
GET /api/v1/interviewer/check-ws-connection?ticket={ticket}
```

Check if a WebSocket connection has been initiated.

**Parameters:**

- `ticket`: The ticket string from the request endpoint

**Response:**

```json
{
  "status": "initiated"
}
```

Possible status values:

- `initiated`: WebSocket connection has been initiated
- `not-initiated`: WebSocket connection has not been initiated
- `unauthorized`: The ticket doesn't belong to the current user

### Process Interview Messages

```
POST /api/v1/interview/messages
```

Process a batch of interview messages. This endpoint allows for sending multiple messages in a single request for processing.

**Request Body:**

```json
{
  "messages": [
    {
      "id": 1,
      "content": "Tell me about your programming experience.",
      "timestamp": "2025-03-08T14:30:00.000Z",
      "role": "assistant"
    },
    {
      "id": 2,
      "content": "I have five years of experience with Java and Spring Boot.",
      "timestamp": "2025-03-08T14:30:30.000Z",
      "role": "user"
    }
  ],
  "accessKey": "abc-1234" // Optional - job access key for linking results
}
```

**Message Fields:**

- `id`: Unique identifier for the message
- `content`: The actual text content of the message
- `timestamp`: ISO-8601 formatted timestamp
- `role`: Role of the message sender (e.g., "user", "assistant", "system")
- `accessKey`: Optional job access key to associate results with a specific job

**Response:**

- `200 OK`: Messages processed successfully with a map of processed results including classifications and evaluations

**Authentication:**

- This endpoint requires authentication. The user's identity is extracted from the security context.
- If authenticated, the user information is logged along with the number of messages received.
- If not authenticated, the request is still processed but logged as coming from an "unauthenticated user".

### Process Interview Messages for Job

```
POST /api/v1/interview/messages/{accessKey}
```

Process a batch of interview messages for a specific job. This is similar to the general message processing endpoint but explicitly ties the messages to a specific job.

**Parameters:**

- `accessKey`: The job access key to associate with the messages

**Request Body:**
Same as the `/api/v1/interview/messages` endpoint, but the accessKey in the path takes precedence over any included in the request body.

**Response:**
Same as the `/api/v1/interview/messages` endpoint.

### Get Communication Results for Job

```
GET /api/v1/interview/communication-results/job/{accessKey}
```

Retrieve all communication evaluation results for a specific job. This endpoint is intended for recruiters to view all communication assessments for a job posting.

**Parameters:**

- `accessKey`: The job access key

**Response (Success - 200 OK):**

```json
{
  "job": {
    "id": 1,
    "title": "Software Engineer",
    "description": "We are looking for a talented software engineer...",
    "address": "123 Main Street, San Francisco, CA 94105",
    "accessKey": "abc-1234",
    "type": "FULL_TIME",
    "shiftType": "DAY_SHIFT",
    "shiftLengthHours": 8,
    "locationType": "ON_SITE",
    "createdAt": "2025-03-08T09:00:00",
    "updatedAt": "2025-03-08T09:00:00",
    "owner": {
      "id": 1,
      "username": "recruiter@example.com",
      "firstName": "Jane",
      "lastName": "Recruiter",
      "isRecruiter": true,
      "companyName": "Acme Inc."
    }
  },
  "communicationResults": [
    {
      "id": 1,
      "user": {
        "id": 3,
        "username": "applicant@example.com",
        "firstName": "John",
        "lastName": "Doe"
      },
      "overallScore": 4.5,
      "strengths": "Clear articulation of ideas. Good use of technical terminology.",
      "areasForImprovement": "Could provide more concrete examples.",
      "createdAt": "2025-03-10T10:00:00",
      "updatedAt": "2025-03-10T10:00:00"
    }
    // additional results
  ],
  "count": 5
}
```

**Response (Error):**

- `404 Not Found`: If the job with the specified access key doesn't exist
- `401 Unauthorized`: If the user is not authenticated
- `403 Forbidden`: If the user is not the owner of the job

### Get User Communication Results for Job

```
GET /api/v1/interview/communication-results/job/{accessKey}/user/{userId}
```

Get a specific user's communication result for a job. This can be used by both recruiters (to see a specific applicant) and applicants (to see their own result).

**Parameters:**

- `accessKey`: The job access key
- `userId`: The user's ID

**Response (Success - 200 OK):**

```json
{
  "id": 1,
  "user": {
    "id": 3,
    "username": "applicant@example.com",
    "firstName": "John",
    "lastName": "Doe"
  },
  "overallScore": 4.5,
  "strengths": "Clear articulation of ideas. Good use of technical terminology.",
  "areasForImprovement": "Could provide more concrete examples.",
  "createdAt": "2025-03-10T10:00:00",
  "updatedAt": "2025-03-10T10:00:00"
}
```

**Response (Error):**

- `404 Not Found`: If the job doesn't exist or no results found
- `401 Unauthorized`: If the user is not authenticated
- `403 Forbidden`: If the user is not authorized to view these results

### Get My Communication Results for Job

```
GET /api/v1/interview/communication-results/my/{accessKey}
```

Convenience endpoint for applicants to get their own communication results for a specific job.

**Parameters:**

- `accessKey`: The job access key

**Response:**
Same as the `/api/v1/interview/communication-results/job/{accessKey}/user/{userId}` endpoint.

**Response (Error):**

- `404 Not Found`: If the job doesn't exist or no results found
- `401 Unauthorized`: If the user is not authenticated

### Get Technical Results for Job

```
GET /api/v1/interview/technical-results/job/{accessKey}
```

Retrieve technical evaluation results for a specific job. This endpoint serves both recruiters and applicants:

- Recruiters (job owners) can view all technical assessments for a job posting
- Applicants can view their own results and aggregated statistics for the job

**Parameters:**

- `accessKey`: The job access key

**Response for Recruiters (Success - 200 OK):**

```json
{
  "job": {
    "id": 1,
    "title": "Software Engineer",
    "description": "We are looking for a talented software engineer...",
    "address": "123 Main Street, San Francisco, CA 94105",
    "accessKey": "abc-1234",
    "type": "FULL_TIME",
    "shiftType": "DAY_SHIFT",
    "shiftLengthHours": 8,
    "locationType": "ON_SITE",
    "createdAt": "2025-03-08T09:00:00",
    "updatedAt": "2025-03-08T09:00:00",
    "owner": {
      "id": 1,
      "username": "recruiter@example.com",
      "firstName": "Jane",
      "lastName": "Recruiter",
      "isRecruiter": true,
      "companyName": "Acme Inc."
    }
  },
  "technicalResults": [
    {
      "id": 1,
      "user": {
        "id": 3,
        "username": "applicant@example.com",
        "firstName": "John",
        "lastName": "Doe"
      },
      "questionText": "Explain RESTful APIs",
      "answerText": "REST stands for Representational State Transfer...",
      "score": 80,
      "feedback": "Good explanation with clear examples of REST principles",
      "errors": "Minor confusion about statelessness concept",
      "createdAt": "2025-03-10T10:05:00",
      "updatedAt": "2025-03-10T10:05:00"
    }
    // additional results
  ],
  "count": 8,
  "overallScore": 72.5,
  "isOwner": true
}
```

**Response for Applicants (Success - 200 OK):**

```json
{
  "job": {
    "id": 1,
    "title": "Software Engineer",
    "description": "We are looking for a talented software engineer...",
    "address": "123 Main Street, San Francisco, CA 94105",
    "accessKey": "abc-1234",
    "type": "FULL_TIME",
    "shiftType": "DAY_SHIFT",
    "shiftLengthHours": 8,
    "locationType": "ON_SITE",
    "createdAt": "2025-03-08T09:00:00",
    "updatedAt": "2025-03-08T09:00:00",
    "owner": {
      "id": 1,
      "username": "recruiter@example.com",
      "firstName": "Jane",
      "lastName": "Recruiter",
      "isRecruiter": true,
      "companyName": "Acme Inc."
    }
  },
  "technicalResults": [
    {
      "id": 5,
      "user": {
        "id": 3,
        "username": "applicant@example.com",
        "firstName": "John",
        "lastName": "Doe"
      },
      "questionText": "Explain RESTful APIs",
      "answerText": "REST stands for Representational State Transfer...",
      "score": 80,
      "feedback": "Good explanation with clear examples of REST principles",
      "errors": "Minor confusion about statelessness concept",
      "createdAt": "2025-03-10T10:05:00",
      "updatedAt": "2025-03-10T10:05:00"
    }
    // only this user's results
  ],
  "userScore": 75.0,
  "averageScore": 68.0,
  "isOwner": false
}
```

**Response (Error):**

- `404 Not Found`: If the job with the specified access key doesn't exist or no results found for the user
- `401 Unauthorized`: If the user is not authenticated

### Get User Technical Results for Job

```
GET /api/v1/interview/technical-results/job/{accessKey}/user/{userId}
```

Get a specific user's technical results for a job. This can be used by both recruiters (to see a specific applicant) and applicants (to see their own results).

**Parameters:**

- `accessKey`: The job access key
- `userId`: The user's ID

**Response (Success - 200 OK):**

```json
{
  "technicalResults": [
    {
      "id": 1,
      "user": {
        "id": 3,
        "username": "applicant@example.com",
        "firstName": "John",
        "lastName": "Doe"
      },
      "questionText": "Explain RESTful APIs",
      "answerText": "REST stands for Representational State Transfer...",
      "score": 8,
      "feedback": "Good explanation with clear examples of REST principles",
      "errors": "Minor confusion about statelessness concept",
      "createdAt": "2025-03-10T10:05:00",
      "updatedAt": "2025-03-10T10:05:00"
    }
    // additional results for the same user
  ],
  "count": 3,
  "overallScore": 73.3,
  "job": {
    "id": 1,
    "title": "Software Engineer",
    "description": "We are looking for a talented software engineer...",
    "address": "123 Main Street, San Francisco, CA 94105",
    "accessKey": "abc-1234",
    "type": "FULL_TIME",
    "shiftType": "DAY_SHIFT",
    "shiftLengthHours": 8,
    "locationType": "ON_SITE",
    "createdAt": "2025-03-08T09:00:00",
    "updatedAt": "2025-03-08T09:00:00",
    "owner": {
      "id": 1,
      "username": "recruiter@example.com",
      "firstName": "Jane",
      "lastName": "Recruiter",
      "isRecruiter": true,
      "companyName": "Acme Inc."
    }
  },
  "isOwner": true // or false if the viewer is the applicant themselves
}
```

**Response (Error):**

- `404 Not Found`: If the job doesn't exist or no results found
- `401 Unauthorized`: If the user is not authenticated
- `403 Forbidden`: If the user is not authorized to view these results

### Get My Technical Results for Job

```
GET /api/v1/interview/technical-results/my/{accessKey}
```

Convenience endpoint for applicants to get their own technical results for a specific job.

**Parameters:**

- `accessKey`: The job access key

**Response:**
Same as the `/api/v1/interview/technical-results/job/{accessKey}/user/{userId}` endpoint, with `isOwner` always set to `false`.

**Response (Error):**

- `404 Not Found`: If the job doesn't exist or no results found
- `401 Unauthorized`: If the user is not authenticated

## Admin Endpoints

Admin endpoints are intended for system administrators and should be properly secured in production environments. Use with caution.

### Regenerate All Interview Results

```
POST /api/v1/interview/admin/regenerate-results
```

Regenerates communication and technical results for all users based on their interview history for a specific access key. This is a maintenance endpoint for administrators to reprocess and update all interview results in the system. **This endpoint should be protected and not exposed in production without proper authentication and authorization.**

**Request Body:**
_None_

**Response (Success - 200 OK):**

```json
{
  "message": "Regeneration process finished.",
  "totalUsersAttempted": 42,
  "successfulRegenerations": 39,
  "failedRegenerations": 3,
  "errors": {
    "12": "User not found",
    "27": "Flask API connection error: Connection refused",
    "35": "No interview history found in Flask API (404)"
  }
}
```

**Response Fields:**

- `message`: Status message for the operation
- `totalUsersAttempted`: Number of users for whom regeneration was attempted
- `successfulRegenerations`: Number of users whose results were successfully regenerated
- `failedRegenerations`: Number of users for whom regeneration failed
- `errors`: Map of user IDs to error messages (if any failures occurred)

**Security Warning:**

> This endpoint is for administrative use only. It can overwrite existing results and may impact reporting or analytics. Ensure it is not accessible to regular users.

## WebSocket Protocol

The WebSocket connection uses a binary protocol for real-time interviewing. The message format is:

```
[4 bytes messageHeader] + [4 bytes message length] + [JSON message bytes] + [binary storage bytes]
```

### Message Types

- `0`: Heartbeat (keep-alive)
- `1`: Client audio send
- `2`: AI audio response
- `3`: Client text send
- `4`: Transcription text
- `5`: Client authentication request

### Client Authentication Message Format

```json
{
  "ticket": "2025-03-08T14:30:15.123Z-987654321"
}
```

### Client Audio Send Message Format

```json
{
  "timestamp": 1709906051234,
  "format": "mp3",
  "duration": 5.2
}
```

Audio data is included as binary storage after the JSON.

### AI Audio Response Message Format

```json
{
  "timestamp": 1709906052345,
  "text": "Tell me about your experience with Java programming.",
  "format": "mp3",
  "duration": 4.8
}
```

Audio data is included as binary storage after the JSON.

### Client Text Send Message Format

```json
{
  "timestamp": 1709906053456,
  "text": "I have five years of experience working with Java."
}
```

### Transcription Text Message Format

```json
{
  "timestamp": 1709906054567,
  "text": "I have five years of experience working with Java.",
  "isFinal": true
}
```

### Connection Flow

1. Obtain a ticket through the REST API
2. Connect to the WebSocket endpoint at `ws://server-address/ws/interview`
3. Send an authentication message (type 5) with the ticket
4. Begin sending/receiving audio and text messages

### Audio Format

Audio data should be sent in MP3 format for optimal processing. The system processes speech using the Whisper API and generates responses through an AI interviewer model.

## Debug Endpoints

For development and testing purposes, there are additional debug endpoints:

### Start Interview

```
POST /debug/interview-start
```

Start a mock interview session.

**Request Body:**

```json
{
  "company": "Acme Inc",
  "role": "Software Engineer",
  "technicalQuestions": [
    "What is your experience with Java?",
    "Explain RESTful APIs",
    "How would you implement a sorting algorithm?"
  ]
}
```

**Response:**
Server-sent events (SSE) stream with the following format:

```json
{
  "order": 0,
  "message": "Hello, I'm Tikki, and I'll be interviewing you today for the Software Engineer role at Acme Inc. How are you doing today?",
  "audioBase64": "base64-encoded-audio-data"
}
```

### Ask Question

```
POST /debug/interview-ask
```

Ask a question during an active mock interview.

**Request Body:**

```json
{
  "response": "I have five years of experience with Java. I've worked on several enterprise applications using Spring Boot and have contributed to open-source Java projects."
}
```

**Response:**
Server-sent events (SSE) stream with the same format as the start interview endpoint.

## Interview State Management

The interviewer maintains session state in Redis to ensure continuity throughout the interview. State includes:

- Company name
- Role
- Technical questions
- Conversation history

Sessions expire after 1 hour of inactivity. To resume an interview, use the same user account.
