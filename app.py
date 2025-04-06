from flask import Flask, request, jsonify
from flask_socketio import SocketIO
from openai import OpenAI
from dotenv import load_dotenv
import os
from functions import check_record, init_db, insert_msg, get_messages, get_history, get_applicants

load_dotenv()

app = Flask(__name__)
socketio = SocketIO(app, cors_allowed_origins="*")
client = OpenAI(
    api_key=os.environ.get("OPENAI_API_KEY"),
)

init_db()

@app.route("/start_interview", methods=["POST"])
def start_interview():
    data = request.get_json()

    access_key = data.get("accessKey")
    name = data.get("name")
    email = data.get("email")
    prompt = data.get("prompt")

    variables = [access_key, name, email, prompt]

    for var in variables:
        if not var:
            return jsonify({"error": "incomplete params"}), 400

    result = check_record(access_key, name, email)
    if result != False:
        return result
    
    insert_msg(prompt, access_key, "system", name, email)

    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{"role": "system", "content": prompt}],
    )

    msg = response.choices[0].message.content

    insert_msg(msg, access_key, "system", name, email)

    response_data = {
        "status": "Interview has started",
        "system": msg,
    }

    return jsonify(response_data), 200


@app.route("/get_messages/<access_key>/<name>/<email>", methods=["GET"])
def messages(access_key, name, email):
    access_key, messages = get_messages(access_key, name, email)
    return jsonify({"status": "success", "acces_key": access_key, "message_count": 0, "messages": messages})


@app.route("/get_applicants/<access_key>", methods=["GET"])
def applicants(access_key):
    applicants = get_applicants(access_key)
    return jsonify({"status": "success", "applicants": applicants}), 200


@app.route("/reply", methods=["POST"])
def reply():
    data = request.get_json()

    access_key = data.get("accessKey")
    name = data.get("name")
    email = data.get("email")
    message = data.get("message")

     # Basic validation
    variables = [access_key, name, email, message]
    for var in variables:
        print(var)
        if not var:
            return jsonify({"error": "incomplete params"}), 400

    insert_msg(message, access_key, "user", name, email)

    messages = get_history(access_key, name, email)
    if not messages:
         return jsonify({"error": "Cannot reply, no interview history found."}), 404

    try:
        response = client.chat.completions.create(model="gpt-4o-mini", messages=messages)

        content = response.choices[0].message.content

        insert_msg(content, access_key, "system", name, email)

        return jsonify({"system": content}), 200

    except Exception as e:
        # Handle potential API errors
        print(f"Error calling OpenAI or processing reply: {e}") # Log error
        return jsonify({"error": "Failed to get response from AI"}), 500

    

if __name__ == "__main__":
    # Initialize the database when the app starts
    socketio.run(app, debug=True)
