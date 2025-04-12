# General API Information

This document provides general information about the Tukma API, including authentication requirements and model definitions.

## Authentication Requirements

Most API endpoints require authentication using the JWT token provided during login. However, the following endpoints are publicly accessible without authentication:

- All authentication endpoints (`/api/v1/auth/**`) except for user job status endpoints
- Job listing for applicants (`/api/v1/jobs/get-all-jobs`)
- Job search endpoint (`/api/v1/jobs/search`)
- Job details (`/api/v1/jobs/get-job-details/{accessKey}`)
- Job metadata (`/api/v1/jobs/job-metadata`)
- Resume duplicates cleanup (`/api/v1/resume/cleanup-duplicates`)
- Debug endpoints (`/debug/**`)
- WebSocket endpoints (`/ws/**`)
- Resume questions endpoints (`/api/v1/survey`)

All other endpoints require a valid JWT token to be included in the request cookies.

## Authentication Flow

1. User logs in via `/api/v1/auth/login`
2. Server returns a JWT token as an HTTP-only cookie
3. JWT token is automatically included in subsequent requests
4. Server validates the token for protected endpoints
5. If token is invalid or expired, server returns 401 Unauthorized

## Common Response Codes

- `200 OK`: Request succeeded
- `201 Created`: Resource successfully created
- `204 No Content`: Request succeeded with no response body
- `400 Bad Request`: Invalid request parameters
- `401 Unauthorized`: Missing or invalid authentication
- `403 Forbidden`: Authenticated but not authorized to access the resource
- `404 Not Found`: Requested resource not found
- `409 Conflict`: Request conflicts with the current state
- `500 Internal Server Error`: Server-side error

## Common Model Information

### User Types
- `Applicant (isApplicant=true)`: A job seeker who can upload resumes and apply to jobs. Has a nullable `hasJob` field that indicates their current employment status (null = unspecified, true = employed, false = unemployed).
- `Recruiter (isApplicant=false)`: A user who can create and manage job postings. Has a `hasJob` field that defaults to false.

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

## Error Handling

The API provides consistent error responses across all endpoints. Error responses include:

```json
{
  "message": "Description of the error",
  "details": "Additional details when available"
}
```

For validation errors, the response includes field-specific errors:

```json
{
  "errors": {
    "email": "Email is required",
    "password": "Password must be at least 8 characters"
  }
}
```

## Rate Limiting

The API implements rate limiting to prevent abuse. Current limits:

- Authentication endpoints: 10 requests per minute
- All other endpoints: 60 requests per minute

When rate limits are exceeded, the API returns:
- Status code: `429 Too Many Requests`
- Response body: `{"message": "Rate limit exceeded. Please try again later."}`

## Cross-Origin Resource Sharing (CORS)

The API supports CORS for integrating with frontend applications. The following origins are allowed:

- `http://localhost:3000` (Local development)
- `http://127.0.0.1:5500` (Local development alternative)

Allowed methods:
- GET
- POST
- PUT
- DELETE

Credentials (cookies) are allowed to be included with requests.

## Data Retention

Tukma maintains the following data retention policies:

- User accounts: Indefinitely, until deleted by the user
- Job postings: 180 days after the posting date (or until manually deleted)
- Resume data: 180 days after submission
- Interview recordings: 30 days after interview completion

## API Versioning

All API endpoints are prefixed with `/api/v1/` to indicate the API version. When breaking changes are introduced, a new version will be released under `/api/v2/`.

## Technologies Used

- Spring Boot 3.4.1
- JWT Authentication
- PostgreSQL Database
- Redis for caching and session management
- OpenAI API for AI services
