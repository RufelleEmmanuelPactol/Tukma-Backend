# Interviewer API

This document details the interviewer endpoints and WebSocket protocol for the Tukma application.

## REST Endpoints

### Request WebSocket Connection

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
  "status": "initiated" | "not-initiated" | "unauthorized"
}
```

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

### Connection Flow

1. Obtain a ticket through the REST API
2. Connect to the WebSocket endpoint with the ticket
3. Send an authentication message (type 5) with the ticket
4. Begin sending/receiving audio and text messages

### Audio Format

Audio data should be sent in MP3 format for optimal processing. The system processes speech using Whisper API and generates responses through an AI interviewer model.

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
    "Explain RESTful APIs"
  ]
}
```

**Response:**
Server-sent events (SSE) stream with audio and text messages.

### Ask Question

```
POST /debug/interview-ask
```

Ask a question during an active mock interview.

**Request Body:**
```json
{
  "response": "I have five years of experience with Java."
}
```

**Response:**
Server-sent events (SSE) stream with audio and text messages.
