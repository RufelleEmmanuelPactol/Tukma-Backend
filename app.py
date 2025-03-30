from flask import Flask, request, jsonify
from flask_socketio import SocketIO, emit, join_room, leave_room
from datetime import datetime
from openai import OpenAI
from pathlib import Path
from dotenv import load_dotenv
import sqlite3, os

load_dotenv()

app = Flask(__name__)
socketio = SocketIO(app, cors_allowed_origins="*")
client = OpenAI(
    # This is the default and can be omitted
    api_key=os.environ.get("OPENAI_API_KEY"),
)
CHUNK_SIZE = 4096
audio_buffers = {}

DATABASE = "messages.db"


# Function to initialize the database
def init_db():
    with sqlite3.connect(DATABASE) as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL,
                date_created TIMESTAMP NOT NULL,
                role TEXT NOT NULL,
                access_key TEXT NOT NULL
            )
        """
        )

        # status 0 = initialized
        # status 1 = finished
        # Create the interview_status table (if it doesn't already exist)
        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS interview_status (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                access_key TEXT NOT NULL,
                status INTEGER NOT NULL
            )
        """
        )
        conn.commit()


# Initialize the database when the app starts
init_db()


@app.route("/start_interview", methods=["POST"])
def start_interview():
    data = request.get_json()

    access_key = data.get("accessKey")
    prompt = data.get("prompt")

    print(access_key)
    print(prompt)

    print("DEBUG1")
    if not access_key or not prompt:
        return jsonify({"error": "incomplete params"}), 400
    print("DEBUG2")
    try:
        # Connect to the database
        with sqlite3.connect(DATABASE) as conn:
            cursor = conn.cursor()

            cursor.execute(
                "SELECT id FROM interview_status WHERE access_key = ?", (access_key,)
            )
            existing_record = cursor.fetchone()
            print("DEBUG3")
            if existing_record:
                return jsonify({"message": "access key already exists"}), 400
            print("DEBUG4")
            cursor.execute(
                """
                    INSERT INTO interview_status (access_key, status)
                    VALUES (?, ?)
                """,
                (access_key, 0),
            )
            conn.commit()
            print(f"New record inserted with access key: {access_key}")
            print("DEBUG5")
            response = client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[{"role": "system", "content": prompt}],
            )

            print("DEBUG6")
            role = "system"
            msg = response.choices[0].message.content
            print(msg)
            cursor.execute(
                """
                INSERT INTO messages (content, date_created, role, access_key)
                VALUES (?, ?, ?, ?)
            """,
                (msg, datetime.now(), role, access_key),
            )
            conn.commit()

            print("DEBUG7")
            filename = datetime.now().strftime("%Y%m%d_%H%M%S") + ".mp3"
            speech_file = Path(__file__).parent / filename
            with client.audio.speech.with_streaming_response.create(
                model="gpt-4o-mini-tts",
                voice="alloy",
                input=msg,
                instructions="Speak in a clear, professional, and neutral tone. Maintain a steady pace and articulate questions precisely.",
            ) as response:
                response.stream_to_file(speech_file)
                chunk_size = 4096  # Adjust the chunk size as necessary.
                print("DEBUG8")
                with open(speech_file, "rb") as f:
                    while True:
                        chunk = f.read(chunk_size)
                        if not chunk:
                            break
                        # Emitting the chunk over the socket.
                        socketio.emit(
                            "audio_chunk", chunk.decode("latin-1"), to=access_key
                        )
                        print("DEBUG9")

            socketio.emit("audio_end", to=access_key)

            print("DEBUG10")
            response_data = {
                "status": "Interview has started",
                "message": msg,
            }
            # Cleanup
            os.remove(speech_file)
        return jsonify(response_data), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/check_status/<access_key>", methods=["GET"])
def check_status(access_key):
    try:
        conn = sqlite3.connect("messages.db")
        cursor = conn.cursor()

        # Query the status for the given access_key
        cursor.execute(
            "SELECT status FROM interview_status WHERE access_key = ?", (access_key,)
        )
        result = cursor.fetchone()

        # Close connection
        conn.close()

        if result:
            status = result[0]
            return (
                jsonify(
                    {
                        "access_key": access_key,
                        "status": status,
                        "message": "Status retrieved successfully",
                    }
                ),
                200,
            )
        else:
            return (
                jsonify({"access_key": access_key, "message": "Access key not found"}),
                404,
            )

    except Exception as e:
        return (
            jsonify(
                {"error": str(e), "message": "An error occurred while checking status"}
            ),
            500,
        )


@app.route("/get_messages/<access_key>", methods=["GET"])
def get_messages(access_key):
    """Retrieve all messages for a given access key, ordered chronologically"""
    conn = None
    try:
        conn = sqlite3.connect("messages.db")
        cursor = conn.cursor()

        # Verify the access key exists
        cursor.execute(
            "SELECT COUNT(*) FROM messages WHERE access_key = ?", (access_key,)
        )
        message_count = cursor.fetchone()[0]

        if message_count == 0:
            return (
                jsonify(
                    {
                        "status": "error",
                        "message": "No messages found for this access key",
                    }
                ),
                404,
            )

        # Retrieve all messages
        cursor.execute(
            """
            SELECT id, content, date_created, role 
            FROM messages 
            WHERE access_key = ?
            ORDER BY date_created ASC
            """,
            (access_key,),
        )

        # Format the response
        messages = [
            {
                "id": row[0],
                "content": row[1],
                "timestamp": row[2],
                "role": row[3],  # 'user' or 'assistant' typically
            }
            for row in cursor.fetchall()
        ]

        return jsonify(
            {
                "status": "success",
                "access_key": access_key,
                "message_count": message_count,
                "messages": messages,
            }
        )

    except sqlite3.Error as e:
        return jsonify({"status": "error", "message": f"Database error: {str(e)}"}), 500

    except Exception as e:
        return (
            jsonify({"status": "error", "message": f"Unexpected error: {str(e)}"}),
            500,
        )

    finally:
        if conn:
            conn.close()


@socketio.on("connect")
def handle_connect():
    print("Client connected")

    emit("open", {"message": "Welcome to the WebSocket server!"})


@socketio.on("disconnect")
def handle_disconnect():
    print("Client disconnected")


@socketio.on("join_room")
def handle_join_room(data):
    room = data["room"]
    join_room(room)  # Add the client to the room
    emit("message", {"data": f"You have entered the room {room}"}, to=room)


@socketio.on("leave_room")
def handle_leave_room(data):
    room = data["room"]
    leave_room(room)  # Remove the client from the room

    emit("message", {"data": f"You left room: {room}"}, to=room)


@socketio.on("room_message_chunk")
def handle_room_message_chunk(data):
    room = data.get("room")
    audio = data.get("audio")
    if room is None or audio is None:
        return
    if room not in audio_buffers:
        audio_buffers[room] = []
    # The audio chunk comes in as binary data (bytes)
    audio_buffers[room].append(audio)
    print(f"Received chunk for room {room}, size: {len(audio)} bytes.")


@socketio.on("room_message_end")
def handle_room_message_end(data):
    room = data["room"]
    filename = datetime.now().strftime("%Y%m%d_%H%M%S") + ".wav"
    if room in audio_buffers:
        # Concatenate all received chunks
        complete_audio_buffer = b"".join(audio_buffers[room])
        print(
            f"Completed audio received for room {room}. Total size: {len(complete_audio_buffer)} bytes"
        )

        # Optionally, save the complete audio data to a file
        with open(filename, "wb") as f:
            f.write(complete_audio_buffer)
        print(f"Saved complete audio to {filename}")

        # Clear the buffer for that room
        del audio_buffers[room]

    transcription = client.audio.transcriptions.create(
        model="gpt-4o-mini-transcribe", file=Path(filename)
    )

    os.remove(filename)

    print(transcription.text)
    with sqlite3.connect(DATABASE) as conn:
        cursor = conn.cursor()

        cursor.execute(
            """
            INSERT INTO messages (content, date_created, role, access_key)
            VALUES (?, ?, ?, ?)
        """,
            (transcription.text, datetime.now(), "user", room),
        )

        conn.commit()

        cursor.execute(
            """
            SELECT * FROM messages
            WHERE access_key = ?
            ORDER BY date_created ASC
        """,
            (room,),
        )

        messages = cursor.fetchall()

    fetch_message("user_message", room)

    history = []

    for message in messages:
        # message structure: (id, content, date_created, role, access_key)
        history.append(
            {
                "role": message[3],  # role is at index 3
                "content": message[1],  # content is at index 1
            }
        )

    print(history)
    response = client.chat.completions.create(model="gpt-4o-mini", messages=history)

    role = "system"
    msg = response.choices[0].message.content
    print(msg)

    cursor.execute(
        """
        INSERT INTO messages (content, date_created, role, access_key)
        VALUES (?, ?, ?, ?)
    """,
        (msg, datetime.now(), role, room),
    )

    conn.commit()

    filename = datetime.now().strftime("%Y%m%d_%H%M%S") + ".mp3"
    speech_file = Path(__file__).parent / filename
    with client.audio.speech.with_streaming_response.create(
        model="gpt-4o-mini-tts",
        voice="alloy",
        input=msg,
        instructions="Speak in a clear, professional, and neutral tone. Maintain a steady pace and articulate questions precisely.",
    ) as response:
        response.stream_to_file(speech_file)
        chunk_size = 4096  # Adjust the chunk size as necessary.
        with open(speech_file, "rb") as f:
            while True:
                chunk = f.read(chunk_size)
                if not chunk:
                    break
                # Emitting the chunk over the socket.
                socketio.emit("audio_chunk", chunk.decode("latin-1"), to=room)

        socketio.emit("audio_end", to=room)

        fetch_message("system_message", room)

        # Cleanup
        os.remove(speech_file)

def fetch_message(name, room):
    conn = None
    try:
        conn = sqlite3.connect("messages.db")
        cursor = conn.cursor()

        # Retrieve all messages
        cursor.execute(
            """
            SELECT id, content, date_created, role 
            FROM messages 
            WHERE access_key = ?
            ORDER BY date_created ASC
            """,
            (room,),
        )

        # Format the response
        messages = [
            {
                "id": row[0],
                "content": row[1],
                "timestamp": row[2],
                "role": row[3],  # 'user' or 'assistant' typically
            }
            for row in cursor.fetchall()
        ]

        socketio.emit(name, messages, to=room)

    except sqlite3.Error as e:
        return jsonify({"status": "error", "message": f"Database error: {str(e)}"}), 500

    except Exception as e:
        return (
            jsonify({"status": "error", "message": f"Unexpected error: {str(e)}"}),
            500,
        )

    finally:
        if conn:
            conn.close()


if __name__ == "__main__":
    socketio.run(app)
