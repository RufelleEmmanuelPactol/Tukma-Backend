package org.tukma.interviewer.controller;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * StandardTukmaMessage is a protocol class designed for WebSocket communication.
 * It encapsulates a message header, an internal JSON string message, and optional binary storage (e.g., MP3).
 * This class provides serialization and deserialization methods for easy transport.
 */
public class StandardTukmaMessage {

    private static final Gson gson = new Gson(); // Singleton Gson instance

    private int messageHeader;         // Message type or identifier
    private Map<String, Object> messageData;  // Internal HashMap (JSON)
    private byte[] nullableStorage;    // Optional binary data (e.g., MP3 file)

    // Message Type Constants
    public static final int HEARTBEAT = 0; // Keep-alive message
    public static final int CLIENT_AUDIO_SEND = 1; // Audio data from client
    public static final int AI_AUDIO_RESPONSE = 2; // Audio data from AI
    public static final int CLIENT_TEXT_SEND = 3; // Text data from client
    public static final int TRANSCRIPTION_TEXT = 4; // Transcription text from AI
    public static final int CLIENT_AUTH_REQUEST = 5; // Authentication request from client

    /**
     * Constructor to create a message with a header, internal JSON message data, and optional storage.
     *
     * @param messageHeader  Integer representing message type or ID.
     * @param messageData    HashMap containing structured message data.
     * @param nullableStorage (Optional) Byte array storage (e.g., binary files).
     */
    public StandardTukmaMessage(int messageHeader, Map<String, Object> messageData, byte[] nullableStorage) {
        this.messageHeader = messageHeader;
        this.messageData = (messageData != null) ? messageData : new HashMap<>();
        this.nullableStorage = nullableStorage;
    }

    /**
     * Constructor to deserialize a byte array back into a StandardTukmaMessage.
     * The byte array follows the format:
     * [4 bytes messageHeader] + [4 bytes message length] + [JSON message bytes] + [binary storage bytes]
     *
     * @param bytes The serialized message as a byte array.
     */
    public StandardTukmaMessage(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        // Read messageHeader (first 4 bytes)
        this.messageHeader = buffer.getInt();

        // Read JSON message length (next 4 bytes)
        int messageLength = buffer.getInt();

        // Read JSON message if available
        if (messageLength > 0) {
            byte[] messageBytes = new byte[messageLength];
            buffer.get(messageBytes);
            String jsonString = new String(messageBytes, StandardCharsets.UTF_8);

            // Convert JSON string to HashMap
            try {
                this.messageData = gson.fromJson(jsonString, Map.class);
            } catch (JsonSyntaxException e) {
                this.messageData = new HashMap<>(); // Fallback to empty map if invalid JSON
            }
        } else {
            this.messageData = new HashMap<>();
        }

        // Read nullableStorage (remaining bytes, if any)
        if (buffer.hasRemaining()) {
            this.nullableStorage = new byte[buffer.remaining()];
            buffer.get(this.nullableStorage);
        } else {
            this.nullableStorage = null;
        }
    }

    /**
     * Serializes the StandardTukmaMessage into a byte array for transmission.
     *
     * Format:
     * [4 bytes messageHeader] + [4 bytes JSON message length] + [JSON message bytes] + [binary storage bytes]
     *
     * @return Serialized byte array representation.
     */
    public byte[] serialize() {
        String jsonString = gson.toJson(this.messageData);
        byte[] messageBytes = jsonString.getBytes(StandardCharsets.UTF_8);
        byte[] storageBytes = (nullableStorage != null) ? nullableStorage : new byte[0];

        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + messageBytes.length + storageBytes.length);
        buffer.putInt(messageHeader);       // Message Header
        buffer.putInt(messageBytes.length); // JSON Message Length
        buffer.put(messageBytes);           // JSON Message Content
        buffer.put(storageBytes);           // Binary Data

        return buffer.array();
    }

    /**
     * Gets the message header.
     *
     * @return Integer message header.
     */
    public int getMessageHeader() {
        return messageHeader;
    }

    /**
     * Gets the message data as a structured HashMap.
     *
     * @return HashMap containing structured JSON message data.
     */
    public Map<String, Object> getMessageData() {
        return messageData;
    }

    /**
     * Gets the optional binary storage.
     *
     * @return Nullable byte array storage.
     */
    public byte[] getNullableStorage() {
        return nullableStorage;
    }

    /**
     * Adds a key-value pair to the internal message data.
     *
     * @param key   The key.
     * @param value The value.
     */
    public void addMessageData(String key, Object value) {
        this.messageData.put(key, value);
    }

    /**
     * Converts the internal HashMap to a JSON string.
     *
     * @return JSON representation of the message data.
     */
    public String toJson() {
        return gson.toJson(this.messageData);
    }

    @Override
    public String toString() {
        return "StandardTukmaMessage{" +
                "messageHeader=" + messageHeader +
                ", messageData=" + messageData +
                ", nullableStorage=" + (nullableStorage != null ? nullableStorage.length + " bytes" : "null") +
                '}';
    }
}