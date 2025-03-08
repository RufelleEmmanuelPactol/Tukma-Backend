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
