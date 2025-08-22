package com.multisensor.recording.protocol;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SchemaManager {
    private static final String TAG = "SchemaManager";
    private static final String SCHEMA_FILE = "message_schema.json";

    private static SchemaManager instance;
    private JSONObject schema;
    private Set<String> validMessageTypes;
    private Context context;

    private SchemaManager(Context context) {
        this.context = context.getApplicationContext();
        loadSchema();
    }

    public static synchronized SchemaManager getInstance(Context context) {
        if (instance == null) {
            instance = new SchemaManager(context);
        }
        return instance;
    }

    private void loadSchema() {
        try {
            InputStream is = context.getAssets().open(SCHEMA_FILE);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String schemaJson = new String(buffer, "UTF-8");
            schema = new JSONObject(schemaJson);

            extractValidMessageTypes();

            Log.i(TAG, "Successfully loaded message schema");

        } catch (IOException e) {
            Log.e(TAG, "Failed to load schema file: " + e.getMessage());
            schema = null;
        } catch (JSONException e) {
            Log.e(TAG, "Invalid JSON in schema file: " + e.getMessage());
            schema = null;
        }
    }

    private void extractValidMessageTypes() {
        validMessageTypes = new HashSet<>();

        if (schema == null) {
            return;
        }

        try {
            JSONArray oneOf = schema.getJSONArray("oneOf");

            for (int i = 0; i < oneOf.length(); i++) {
                JSONObject messageType = oneOf.getJSONObject(i);

                if (messageType.has("allOf")) {
                    JSONArray allOf = messageType.getJSONArray("allOf");

                    for (int j = 0; j < allOf.length(); j++) {
                        JSONObject part = allOf.getJSONObject(j);

                        if (part.has("properties")) {
                            JSONObject properties = part.getJSONObject("properties");

                            if (properties.has("type")) {
                                JSONObject typeProperty = properties.getJSONObject("type");

                                if (typeProperty.has("const")) {
                                    String messageTypeName = typeProperty.getString("const");
                                    validMessageTypes.add(messageTypeName);
                                }
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "Extracted " + validMessageTypes.size() + " valid message types");

        } catch (JSONException e) {
            Log.e(TAG, "Error extracting message types: " + e.getMessage());
        }
    }

    public boolean validateMessage(JSONObject message) {
        if (schema == null) {
            Log.e(TAG, "Schema not loaded");
            return false;
        }

        if (message == null) {
            Log.e(TAG, "Message is null");
            return false;
        }

        if (!message.has("type")) {
            Log.e(TAG, "Message missing required 'type' field");
            return false;
        }

        if (!message.has("timestamp")) {
            Log.e(TAG, "Message missing required 'timestamp' field");
            return false;
        }

        try {
            String messageType = message.getString("type");

            if (!validMessageTypes.contains(messageType)) {
                Log.w(TAG, "Unknown message type: " + messageType);
                return true;
            }

            return validateMessageType(message, messageType);

        } catch (JSONException e) {
            Log.e(TAG, "Error validating message: " + e.getMessage());
            return false;
        }
    }

    private boolean validateMessageType(JSONObject message, String messageType) {
        switch (messageType) {
            case "start_record":
                return message.has("session_id");

            case "stop_record":
                return message.has("session_id");

            case "preview_frame":
                return message.has("frame_id") &&
                        message.has("image_data") &&
                        message.has("width") &&
                        message.has("height");

            case "file_chunk":
                return message.has("file_id") &&
                        message.has("chunk_index") &&
                        message.has("total_chunks") &&
                        message.has("chunk_data") &&
                        message.has("chunk_size") &&
                        message.has("file_type");

            case "device_status":
                return message.has("device_id") &&
                        message.has("status");

            case "ack":
                return message.has("message_id") &&
                        message.has("success");

            case "calibration_start":
                return message.has("pattern_type") &&
                        message.has("pattern_size");

            case "calibration_result":
                return message.has("success");

            default:
                Log.w(TAG, "Unknown message type for validation: " + messageType);
                return true;
        }
    }

    public List<String> getValidMessageTypes() {
        return new ArrayList<>(validMessageTypes);
    }

    public JSONObject createMessage(String messageType) {
        JSONObject message = new JSONObject();

        try {
            message.put("type", messageType);
            message.put("timestamp", System.currentTimeMillis());

            switch (messageType) {
                case "start_record":
                case "stop_record":
                    message.put("session_id", "");
                    break;

                case "preview_frame":
                    message.put("frame_id", 0);
                    message.put("image_data", "");
                    message.put("width", 0);
                    message.put("height", 0);
                    break;

                case "file_chunk":
                    message.put("file_id", "");
                    message.put("chunk_index", 0);
                    message.put("total_chunks", 0);
                    message.put("chunk_data", "");
                    message.put("chunk_size", 0);
                    message.put("file_type", "video");
                    break;

                case "device_status":
                    message.put("device_id", "");
                    message.put("status", "idle");
                    break;

                case "ack":
                    message.put("message_id", "");
                    message.put("success", true);
                    break;

                case "calibration_start":
                    message.put("pattern_type", "chessboard");
                    JSONObject patternSize = new JSONObject();
                    patternSize.put("rows", 7);
                    patternSize.put("cols", 6);
                    message.put("pattern_size", patternSize);
                    break;

                case "calibration_result":
                    message.put("success", false);
                    break;
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error creating message: " + e.getMessage());
        }

        return message;
    }

    public JSONObject createMessage(String messageType, JSONObject additionalFields) {
        JSONObject message = createMessage(messageType);

        if (additionalFields != null) {
            try {
                for (java.util.Iterator<String> keys = additionalFields.keys(); keys.hasNext(); ) {
                    String key = keys.next();
                    message.put(key, additionalFields.get(key));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error adding additional fields: " + e.getMessage());
            }
        }

        return message;
    }

    public void reloadSchema() {
        loadSchema();
    }

    public boolean isSchemaLoaded() {
        return schema != null;
    }
}
