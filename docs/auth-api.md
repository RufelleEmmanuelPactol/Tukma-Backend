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
    "companyName": "Acme Inc." // Only present for recruiter accounts
  }
}
```

## Field Requirements

### Sign Up - Applicants
- `email`: Required - Must be a unique email address
- `password`: Required - User's password
- `firstName`: Required - User's first name
- `lastName`: Required - User's last name
- `isApplicant`: Required - Must be set to `true` for applicants

### Sign Up - Recruiters
- `email`: Required - Must be a unique email address
- `password`: Required - User's password
- `firstName`: Required - User's first name
- `lastName`: Required - User's last name
- `isApplicant`: Required - Must be set to `false` for recruiters
- `companyName`: Required for recruiters - Company or organization name

### Login
- `email`: Required - Registered email address
- `password`: Required - User's password
