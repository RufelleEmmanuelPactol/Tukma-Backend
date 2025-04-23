from flask import Flask, request, jsonify, url_for, send_from_directory
from openai import OpenAI
from dotenv import load_dotenv
from pathlib import Path
from datetime import datetime
import os
from functions import *

load_dotenv()

app = Flask(__name__)
client = OpenAI(
    api_key=os.environ.get("OPENAI_API_KEY"),
)

init_db()

ALLOWED_EXTENSIONS = {'pdf'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

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
    
    initial_msg(prompt, access_key, "system", name, email)

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

        
@app.route("/finished_interviews/<access_key>", methods=["GET"])
def finished_interviews(access_key):
    finished_interviews = done_interviews(access_key)
    return jsonify({"status": "success", "finished_interviews": finished_interviews })


@app.route("/interview_status/<access_key>/<name>/<email>", methods=["GET"])
def is_finished(access_key, name, email):
    stats = check_interview(access_key, name, email)
    return jsonify({"status": stats})

    
@app.route("/debug", methods=["GET"])
def debug_route():
    messages, resume = debug()
    return jsonify({"messages": messages, "resume": resume})


@app.route("/generate_audio", methods=["POST"])
def generate_audio():
    data = request.get_json()
    text = data.get("text")

    if not text:
        return jsonify({"error": "no text provided"}), 400
        
    filename = datetime.now().strftime("%Y%m%d_%H%M%S") + ".mp3"
    speech_file = Path(AUDIO_DIR)/ filename
    try:
        with client.audio.speech.with_streaming_response.create(
            model="gpt-4o-mini-tts",
            voice="alloy",
            input=text,
            instructions="Speak in a calm, confident tone as if you're hosting a professional interview or podcast. Use natural pacing and emphasize key phrases slightly to keep the listener engaged.",
        ) as response:
            response.stream_to_file(speech_file)

        delete_old_files()

        audio_url = url_for("serve_audio", filename=filename, _external=True)


        return jsonify({"audio_url": audio_url}), 200

    except Exception as e:
        # Handle potential API errors
        print(f"Error calling OpenAI or processing reply: {e}") # Log error
        return jsonify({"error": "Failed to get response from AI"}), 500

        
@app.route("/audio/<filename>")
def serve_audio(filename):
    return send_from_directory(AUDIO_DIR, filename)


@app.route("/delete_history/<access_key>/<email>/<secret_key>")
def route_delete_history(access_key, email, secret_key):
    success, result, rows = delete_history(access_key, email, secret_key)
    return jsonify({"success": success, "result": result, "rows_deleted": rows}), 200 if success else 400
        

@app.route("/resume/upload", methods=["POST"])
def route_upload():
    access_key = request.form.get("access_key")
    email = request.form.get("email")

    if not access_key or not email:
        return jsonify({'error': 'access_key and email must not be empty.'}), 400

    if "file" not in request.files:
        return jsonify({'error': 'No file part'}), 400

    file = request.files['file']
    
    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400

    if not (file and allowed_file(file.filename)):
        return jsonify({"error": "Invalid file type. Only PDF files are allowed"}), 400

    success = save_pdf(access_key, email, file)

    if not success:
        return jsonify({"error": "Resume content already exists"}), 400
        
    return jsonify({"result": "Resume content successfully saved"}), 200

    
@app.route("/resume/<access_key>/<email>")
def route_get_resume(access_key, email):
    content = get_resume(access_key, email)

    if content is False:
        return jsonify({"error": "No resume content found"}), 400
    
    return jsonify({"content": content}), 200


@app.route("/delete_resume/<access_key>/<email>/<secret_key>")
def route_delete_resume(access_key, email, secret_key):
    message, code = delete_resume(access_key, email, secret_key)
    return jsonify({"result": message}), code
        

if __name__ == "__main__":
    # Initialize the database when the app starts
    app.run(debug=True)
