package org.tukma.resume.converters;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.Map;

/**
 * Converter for handling the storage and retrieval of resume result data,
 * which can be complex JSON structures with variable keys.
 * This stores the JSON as a TEXT column in the database.
 */
@Converter
public class ResumeResultConverter implements AttributeConverter<Map<String, Object>, String> {

    private final Gson gson = new Gson();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        
        return gson.toJson(attribute);
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            // First try to parse as a JSON object
            JsonObject jsonObject = gson.fromJson(dbData, JsonObject.class);
            Map<String, Object> resultMap = new HashMap<>();
            
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                
                if (value.isJsonObject()) {
                    // For nested objects like {'keyword': {'similarity_score': 0.5, 'best_matching_ngram': 'text'}}
                    JsonObject nestedObj = value.getAsJsonObject();
                    Map<String, Object> nestedMap = new HashMap<>();
                    
                    for (Map.Entry<String, JsonElement> nestedEntry : nestedObj.entrySet()) {
                        String nestedKey = nestedEntry.getKey();
                        JsonElement nestedValue = nestedEntry.getValue();
                        
                        if (nestedValue.isJsonPrimitive()) {
                            if (nestedValue.getAsJsonPrimitive().isNumber()) {
                                nestedMap.put(nestedKey, nestedValue.getAsDouble());
                            } else {
                                nestedMap.put(nestedKey, nestedValue.getAsString());
                            }
                        } else {
                            nestedMap.put(nestedKey, nestedValue.toString());
                        }
                    }
                    
                    resultMap.put(key, nestedMap);
                } else {
                    // For simple key-value pairs
                    if (value.isJsonPrimitive()) {
                        if (value.getAsJsonPrimitive().isNumber()) {
                            resultMap.put(key, value.getAsDouble());
                        } else {
                            resultMap.put(key, value.getAsString());
                        }
                    } else {
                        resultMap.put(key, value.toString());
                    }
                }
            }
            
            return resultMap;
        } catch (JsonSyntaxException e) {
            // If it's not valid JSON, just store it as a string
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("raw", dbData);
            return resultMap;
        }
    }
}
