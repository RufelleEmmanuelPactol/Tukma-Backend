# Authentication API

This document details the authentication endpoints for the Tukma application.

## Endpoints

### Sign Up

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
  "isApplicant": true,                   // required
  "hasJob": null                        // optional
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
  "companyName": "Acme Inc.",             // required for recruiters
  "hasJob": false                        // optional, defaults to false for recruiters
}
```

**Response:**
- `200 OK`: Account created successfully
- `409 Conflict`: User already exists
- `500 Internal Server Error`: Server error

### Login

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

### User Status

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
    "companyName": "Acme Inc.", // Only present for recruiter accounts
    "hasJob": true           // true, false, or null
  }
}
```

### Update Job Status

```
POST /api/v1/auth/update-job-status
```

Update the hasJob field for the currently authenticated user.

**Request Body:**
```json
{
  "hasJob": true    // Boolean value (true or false) or null
}
```

**Response:**
```json
{
  "userDetails": {
    "id": 1,
    "username": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "isRecruiter": false,
    "companyName": null,
    "hasJob": true
  }
}
```

### Get Users by Job Status

```
GET /api/v1/auth/users-by-job-status?hasJob=[true|false|null]
```

Get users filtered by their hasJob status. This endpoint is only accessible to recruiters.

**Parameters:**
- `hasJob` (optional): Filter users by job status (true, false). If not provided, returns users with null hasJob status.

**Response:**
```json
{
  "users": [
    {
      "id": 1,
      "username": "applicant1@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "isRecruiter": false,
      "hasJob": true
    },
    {
      "id": 2,
      "username": "applicant2@example.com",
      "firstName": "Jane",
      "lastName": "Smith",
      "isRecruiter": false,
      "hasJob": true
    }
  ],
  "count": 2
}
```

### Batch Update Job Status

```
POST /api/v1/auth/batch-update-job-status
```

Batch update hasJob status for multiple users. This endpoint is only accessible to recruiters.

**Request Body:**
```json
{
  "updates": [
    {
      "userId": 1,
      "hasJob": true
    },
    {
      "userId": 2,
      "hasJob": false
    },
    {
      "userId": 3,
      "hasJob": null
    }
  ]
}
```

**Response:**
```json
{
  "message": "Batch update completed",
  "updatedUsers": 3
}
```

## Field Requirements

### Sign Up - Applicants
- `email`: Required - Must be a unique email address
- `password`: Required - User's password
- `firstName`: Required - User's first name
- `lastName`: Required - User's last name
- `isApplicant`: Required - Must be set to `true` for applicants
- `hasJob`: Optional - Can be true, false, or null (defaults to null for applicants)

### Sign Up - Recruiters
- `email`: Required - Must be a unique email address
- `password`: Required - User's password
- `firstName`: Required - User's first name
- `lastName`: Required - User's last name
- `isApplicant`: Required - Must be set to `false` for recruiters
- `companyName`: Required for recruiters - Company or organization name
- `hasJob`: Optional - Can be true, false, or null (defaults to false for recruiters)

### Login
- `email`: Required - Registered email address
- `password`: Required - User's password

### Update Job Status
- `hasJob`: Required - Boolean value (true or false) or null

### Batch Update Job Status
- `updates`: Required - Array of objects with userId and hasJob fields
