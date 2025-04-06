from flask import jsonify
from datetime import datetime
import sqlite3

PROD_DB = "/app/data/messages.db"
LOCAL_DB = "./app/data/messages.db"
DATABASE = PROD_DB # change depending on the environment

def init_db():
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
                    email TEXT NOT NULL -- Removed the trailing comma
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
    with sqlite3.connect(DATABASE) as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            INSERT INTO messages (content, date_created, role, access_key, name, email)
            VALUES (?, ?, ?, ?, ?, ?)
        """,
            (content, datetime.now(), role, access_key, name, email),
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
            SELECT DISTINCT name, email
            FROM messages 
            WHERE access_key = ? 
            ORDER BY name, email ASC -- Ordering might not be meaningful with DISTINCT name, email
            """,                       # Consider ORDER BY name, email if needed
            (access_key,),
        )
        # Fetch results *inside* the 'with' block
        applicants_list = cursor.fetchall() 

    # Format if desired, e.g., list of dicts [{'name': n, 'email': e}, ...]
    formatted_applicants = [{"name": name, "email": email} for name, email in applicants_list]

    # Added colon in status key name for consistency
    return formatted_applicants