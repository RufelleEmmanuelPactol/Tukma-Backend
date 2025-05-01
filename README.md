# Tukma AI Interviewer API

## Overview

Tukma AI Interviewer is a Flask-based REST API that facilitates automated job interviews using OpenAI's GPT models. This service is part of the larger Tukma research project designed to filter and select suitable job applicants.

## System Requirements

- Python 3.6+
- Flask
- OpenAI Python client
- SQLite database
- Flask-SocketIO
- python-dotenv
- gunicorn (for production)

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd tukma-flask
```

2. Create a virtual environment and activate it:
```bash
python -m venv venv
# On Windows
venv\Scripts\activate
# On Unix or MacOS
source venv/bin/activate
```

3. Install dependencies:
```bash
pip install -r requirements.txt
```

4. Set up environment variables:

Create a `.env` file in the root directory with the following content:
```
OPENAI_API_KEY=your_openai_api_key
```

## Database

The application uses SQLite to store interview messages. The database file `messages.db` will be created automatically when the application starts.

## API Endpoints

### 1. Start a New Interview

Initiates a new AI interview session for an applicant.

- **URL**: `/start_interview`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "accessKey": "string", // Unique identifier for the interview session
    "name": "string",      // Applicant's name
    "email": "string",     // Applicant's email
    "prompt": "string"     // System prompt for the AI interviewer
  }
  ```
- **Success Response**:
  - **Code**: 200 OK
  - **Content**:
    ```json
    {
      "status": "Interview has started",
      "system": "AI's initial message"
    }
    ```
- **Error Response**:
  - **Code**: 400 Bad Request
  - **Content**:
    ```json
    {
      "error": "incomplete params"
    }
    ```
  - **Code**: 400 Bad Request
  - **Content**:
    ```json
    {
      "message": "Interview already started or exists for this user"
    }
    ```

### 2. Get Interview Messages

Retrieves all messages from a specific interview session.

- **URL**: `/get_messages/<access_key>/<name>/<email>`
- **Method**: `GET`
- **URL Parameters**:
  - `access_key`: Unique identifier for the interview session
  - `name`: Applicant's name
  - `email`: Applicant's email
- **Success Response**:
  - **Code**: 200 OK
  - **Content**:
    ```json
    {
      "status": "success",
      "access_key": "string",
      "messages": [
        {
          "id": 1,
          "content": "Message text",
          "timestamp": "2025-04-06 12:00:00",
          "role": "system" or "user"
        }
      ]
    }
    ```

### 3. Get Applicants

Retrieves all applicants for a specific access key.

- **URL**: `/get_applicants/<access_key>`
- **Method**: `GET`
- **URL Parameters**:
  - `access_key`: Unique identifier for the interview session
- **Success Response**:
  - **Code**: 200 OK
  - **Content**:
    ```json
    {
      "status": "success",
      "applicants": [
        {
          "name": "Applicant Name",
          "email": "applicant@example.com"
        }
      ]
    }
    ```

### 4. Reply to Message

Sends an applicant's message to the AI interviewer and returns the AI's response.

- **URL**: `/reply`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "accessKey": "string", // Unique identifier for the interview session
    "name": "string",      // Applicant's name
    "email": "string",     // Applicant's email
    "message": "string"    // Applicant's message
  }
  ```
- **Success Response**:
  - **Code**: 200 OK
  - **Content**:
    ```json
    {
      "system": "AI's response message"
    }
    ```
- **Error Response**:
  - **Code**: 400 Bad Request
  - **Content**:
    ```json
    {
      "error": "incomplete params"
    }
    ```
  - **Code**: 404 Not Found
  - **Content**:
    ```json
    {
      "error": "Cannot reply, no interview history found."
    }
    ```
  - **Code**: 500 Internal Server Error
  - **Content**:
    ```json
    {
      "error": "Failed to get response from AI"
    }
    ```

## Running the Application

### Development Mode

```bash
python app.py
```

The application will run at http://127.0.0.1:5000/ with debug mode enabled.

### Production Mode

Using Gunicorn (as configured in nixpacks.toml):

```bash
gunicorn app:app
```

## Notes

- The application uses OpenAI's `gpt-4o-mini` model for generating responses
- All interview messages are stored in a SQLite database
- The system handles duplicate interview checks to prevent creating multiple interviews for the same user

## Known Issues and Limitations

1. The `message_count` field in the `/get_messages` endpoint response always returns 0
2. There's a typo in the response where "access_key" is spelled as "acces_key"

## Security Considerations

- This API does not implement authentication for API calls
- Ensure your OpenAI API key is kept secure and not exposed in your codebase
- Consider implementing rate limiting for production use
