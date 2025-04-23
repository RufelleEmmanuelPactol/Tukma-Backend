from flask import jsonify
from pathlib import Path
from datetime import datetime
import sqlite3
import json
import os
import time

PROD_DB = "/app/tukma/messages.db"
LOCAL_DB = "./app/tukma/messages.db"
PROD_AUDIO = "/app/tukma/audio/"
LOCAL_AUDIO = "./app/tukma/audio/"

DATABASE = PROD_DB 
AUDIO_DIR = PROD_AUDIO

def init_db():
    db_dir = os.path.dirname(DATABASE)
    try:
        os.makedirs(db_dir, exist_ok=True)
    except Exception as e:
        print(f"Error creating database directory: {e}")

    audio_dir = os.path.dirname(AUDIO_DIR)
    try:
        os.makedirs(audio_dir, exist_ok=True)
    except Exception as e:
        print(f"Error creating audio directory: {e}")

    conn = None # Initialize conn outside try for the finally block
    try:
        # Use a specific path for the database
        with sqlite3.connect(DATABASE) as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    content TEXT NOT NULL,
                    date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Added default
                    role TEXT NOT NULL,
                    access_key TEXT NOT NULL,
                    name TEXT NOT NULL,
                    email TEXT NOT NULL,
                    is_finished INTEGER NOT NULL
                )
                """
            )
            # Commit is generally good practice after DDL (Data Definition Language) like CREATE TABLE
            conn.commit()

    except sqlite3.Error as e:
        print(f"Database error during init_db: {e}")
        # Re-raise the exception or handle it as appropriate for your app
        raise


def insert_msg(content, access_key, role, name, email):
    is_finished = 0
    phrase = "Thank you for your time and insights"

    if phrase.lower() in content.lower() and role == "system":
        is_finished = 1

    with sqlite3.connect(DATABASE) as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            INSERT INTO messages (content, date_created, role, access_key, name, email, is_finished)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
            (content, datetime.now(), role, access_key, name, email, is_finished),
        )
        conn.commit()


def initial_msg(content, access_key, role, name, email):
    with sqlite3.connect(DATABASE) as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            INSERT INTO messages (content, date_created, role, access_key, name, email, is_finished)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
            (content, datetime.now(), role, access_key, name, email, 0),
        )
        conn.commit()

        
        
def check_record(access_key, name, email):
    with sqlite3.connect(DATABASE) as conn:
        cursor = conn.cursor()

        cursor.execute("""
            SELECT id FROM messages 
            WHERE access_key = ? AND email = ? AND name = ?
        """, (access_key, email, name))
        existing_record = cursor.fetchone()

        if existing_record:
            return jsonify({"message": "Interview already started or exists for this user"}), 400 
        return False


def get_messages(access_key, name, email):
    with sqlite3.connect(DATABASE) as conn:
        cursor = conn.cursor()

        # Retrieve all messages
        cursor.execute(
            """
            SELECT id, content, date_created, role 
            FROM messages 
            WHERE access_key = ? AND email = ? AND name = ? 
            ORDER BY date_created ASC
            """,
            (access_key, email, name),
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

        return access_key, messages

        
def get_history(access_key, name, email):
    history = []
    with sqlite3.connect(DATABASE) as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            SELECT role, content
            FROM messages 
            WHERE access_key = ? AND email = ? AND name = ? 
            ORDER BY date_created ASC
            """,
            (access_key, email, name),
        )
        # Fetch results *inside* the 'with' block
        rows = cursor.fetchall() 
        # Format into list of dictionaries
        history = [{"role": row[0], "content": row[1]} for row in rows]

    return history # Return the formatted list

    
def get_applicants(access_key):
    applicants_list = []
    with sqlite3.connect(DATABASE) as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            SELECT DISTINCT name, email, is_finished
            FROM messages 
            WHERE access_key = ? 
            ORDER BY name, email ASC -- Ordering might not be meaningful with DISTINCT name, email
            """,                       # Consider ORDER BY name, email if needed
            (access_key,),
        )
        # Fetch results *inside* the 'with' block
        applicants_list = cursor.fetchall() 

    # Format if desired, e.g., list of dicts [{'name': n, 'email': e}, ...]
    formatted_applicants = [{"name": name, "email": email, "is_finished": is_finished} for name, email, is_finished in applicants_list]

    # Added colon in status key name for consistency
    return formatted_applicants
    
    
def done_interviews(access_key):
    with sqlite3.connect(DATABASE) as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            SELECT DISTINCT name, email
            FROM messages 
            WHERE access_key = ? AND is_finished = 1
            """,
            (access_key,)
        )
        finished_interview = cursor.fetchall()
        formatted = [{"name": name, "email": email} for name, email in finished_interview]
    return formatted

    
def check_interview(access_key, name, email):
    with sqlite3.connect(DATABASE) as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            SELECT 1
            FROM messages 
            WHERE access_key = ?
              AND name = ?
              AND email = ?
              AND is_finished = 1
            LIMIT 1
            """,
            (access_key, name, email)
        )
        isFinished = cursor.fetchone()

        cursor.execute(
            """
            SELECT 1
            FROM messages 
            WHERE access_key = ?
              AND name = ?
              AND email = ?
              AND is_finished = 0
            LIMIT 1
            """,
            (access_key, name, email)
        )
        isStarted = cursor.fetchone()

        if isFinished is not None:
            return "finished"

        if isStarted is not None:
            return "started"

    return "uninitiated"


def debug():
    with sqlite3.connect(DATABASE) as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM messages")
        results = cursor.fetchall()  # Fetch all results
        
    return results  # Returns a list of all records in the messages table

    
def load_db():
    with open('db.json', 'r', encoding='utf-8') as f:
        messages = json.load(f)
        rows = messages.get('result', [])

    conn = sqlite3.connect(LOCAL_DB)
    c = conn.cursor()   

    # === Insert rows ===
    # If you want to preserve the JSON's id values, include 'id' in your INSERT; otherwise, omit it to let SQLite auto-assign.
    insert_sql = """
    INSERT INTO messages (
        id,
        content,
        date_created,
        role,
        access_key,
        name,
        email,
        is_finished
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """

    for row in rows:
        # row format: [id, content, date_created, role, access_key, name, email, is_finished]
        msg_id, content, date_str, role, access_key, name, email, is_finished = row

        # Optionally parse / validate the timestamp
        try:
            # this ensures it's a valid datetime
            dt = datetime.fromisoformat(date_str)
        except ValueError:
            # fallback to NOW()
            dt = datetime.now()

        c.execute(insert_sql, (
            msg_id,
            content,
            dt.isoformat(sep=' '),
            role,
            access_key,
            name,
            email,
            is_finished
        ))

    conn.commit()
    conn.close()

    
def delete_history(access_key, email, secret_key):
    if secret_key != os.environ.get("SECRET_KEY"):
        return False, "Invalid secret_key", 0

    conn = sqlite3.connect(LOCAL_DB)
    c = conn.cursor()   

    c.execute(
        """
        DELETE FROM messages
        WHERE access_key = ? AND email = ?
        """,
        (access_key, email)
    )
    result_msg = ""

    if c.rowcount > 0:
        result_msg = "Deleted chat history of " + email + " in access_key: " + access_key
    else:
        result_msg = "Incorrect email/access_key or no record"

    conn.commit()
    conn.close()

    return True, result_msg, c.rowcount

    
def delete_all_except_finished(access_key):
    with sqlite3.connect(DATABASE) as conn:
        cursor = conn.cursor()

        # Step 1: Get finished interviews
        cursor.execute(
            """
            SELECT DISTINCT name, email
            FROM messages 
            WHERE access_key = ? AND is_finished = 1
            """,
            (access_key,)
        )
        finished_interview = cursor.fetchall()  # list of (name, email)

        # If there are no finished interviews, delete all for this access_key
        if not finished_interview:
            cursor.execute(
                "DELETE FROM messages WHERE access_key = ?",
                (access_key,)
            )
            print(f"Deleted all messages for access_key={access_key} (no finished interviews).")
            return

        # Step 2: Build WHERE clause to keep only those in finished_interview
        placeholders = ",".join(["(?, ?)"] * len(finished_interview))
        params = [access_key]
        for name, email in finished_interview:
            params.extend([name, email])

        query = f"""
        DELETE FROM messages
        WHERE access_key = ?
        AND (name, email) NOT IN ({placeholders})
        """
        cursor.execute(query, params)
        print(f"Deleted all unfinished/intermediate messages for access_key={access_key}, except finished ones.")

    
def delete_old_files():
    current_time = time.time()
    for file in Path(AUDIO_DIR).glob("*.mp3"):
        if current_time - file.stat().st_mtime > 60:  # Files older than 1 minute
            try:
                os.remove(file)
                print(f"[✓] Deleted {file}")
            except Exception as e:
                print(f"[!] Error deleting {file}: {e}")