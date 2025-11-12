package com.pradeepl.evidence.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * Utility for logging MCP (Model Context Protocol) JSON-RPC messages.
 *
 * This logger provides formatted output for MCP tool calls, responses, and resource access,
 * making it easy to debug and monitor MCP interactions.
 */
public class McpLogger {

    private static final Logger logger = LoggerFactory.getLogger("MCP_MESSAGES");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final boolean ENABLE_PRETTY_PRINT = true;
    private static final int MAX_RESPONSE_LENGTH = 5000; // Max chars to log for responses

    /**
     * Log an incoming MCP tool call with its arguments
     *
     * @param toolName The name of the MCP tool being called
     * @param arguments Map of argument names to values
     */
    public static void logToolCall(String toolName, Map<String, Object> arguments) {
        try {
            logger.info("═══════════════════════════════════════════════");
            logger.info("📥 MCP TOOL CALL: {}", toolName);
            logger.info("───────────────────────────────────────────────");
            logger.info("Timestamp: {}", Instant.now());

            String argsJson = ENABLE_PRETTY_PRINT
                ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(arguments)
                : mapper.writeValueAsString(arguments);

            logger.info("Arguments:\n{}", argsJson);
            logger.info("═══════════════════════════════════════════════\n");

        } catch (Exception e) {
            logger.warn("Failed to log tool call for {}", toolName, e);
        }
    }

    /**
     * Log an MCP tool response
     *
     * @param toolName The name of the MCP tool
     * @param response The response string (typically JSON)
     * @param success Whether the tool execution was successful
     */
    public static void logToolResponse(String toolName, String response, boolean success) {
        try {
            logger.info("═══════════════════════════════════════════════");
            logger.info("📤 MCP TOOL RESPONSE: {} - {}", toolName, success ? "✅ SUCCESS" : "❌ ERROR");
            logger.info("───────────────────────────────────────────────");
            logger.info("Timestamp: {}", Instant.now());

            // Try to pretty-print JSON
            try {
                JsonNode json = mapper.readTree(response);
                String prettyJson = ENABLE_PRETTY_PRINT
                    ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json)
                    : mapper.writeValueAsString(json);

                // Truncate if too long
                if (prettyJson.length() > MAX_RESPONSE_LENGTH) {
                    logger.info("Response (truncated):\n{}\n... (truncated {} characters)",
                        prettyJson.substring(0, MAX_RESPONSE_LENGTH),
                        prettyJson.length() - MAX_RESPONSE_LENGTH);
                } else {
                    logger.info("Response:\n{}", prettyJson);
                }

                // Log summary info
                if (json.has("error")) {
                    logger.warn("Error in response: {}", json.get("error"));
                }

            } catch (Exception e) {
                // Not JSON, log as-is
                if (response.length() > MAX_RESPONSE_LENGTH) {
                    logger.info("Response (truncated): {}\n... (truncated {} characters)",
                        response.substring(0, MAX_RESPONSE_LENGTH),
                        response.length() - MAX_RESPONSE_LENGTH);
                } else {
                    logger.info("Response: {}", response);
                }
            }

            logger.info("═══════════════════════════════════════════════\n");

        } catch (Exception e) {
            logger.warn("Failed to log tool response for {}", toolName, e);
        }
    }

    /**
     * Log an MCP resource access
     *
     * @param resourceUri The URI of the resource being accessed
     * @param resourceName The display name of the resource
     * @param serviceName Optional service name parameter (can be null)
     */
    public static void logResourceAccess(String resourceUri, String resourceName, String serviceName) {
        try {
            logger.info("═══════════════════════════════════════════════");
            logger.info("📚 MCP RESOURCE ACCESS");
            logger.info("───────────────────────────────────────────────");
            logger.info("Timestamp: {}", Instant.now());
            logger.info("URI: {}", resourceUri);
            logger.info("Name: {}", resourceName);
            if (serviceName != null && !serviceName.isEmpty()) {
                logger.info("Service: {}", serviceName);
            }
            logger.info("═══════════════════════════════════════════════\n");

        } catch (Exception e) {
            logger.warn("Failed to log resource access for {}", resourceUri, e);
        }
    }

    /**
     * Log an MCP resource response
     *
     * @param resourceUri The URI of the resource
     * @param contentLength Length of the content returned
     * @param success Whether the resource access was successful
     */
    public static void logResourceResponse(String resourceUri, int contentLength, boolean success) {
        try {
            logger.info("📤 MCP RESOURCE RESPONSE: {} - {}", resourceUri, success ? "✅ SUCCESS" : "❌ ERROR");
            logger.info("Content Length: {} characters", contentLength);
            logger.info("═══════════════════════════════════════════════\n");

        } catch (Exception e) {
            logger.warn("Failed to log resource response for {}", resourceUri, e);
        }
    }

    /**
     * Log an error that occurred during MCP processing
     *
     * @param operation The operation being performed (e.g., "fetch_logs")
     * @param error The exception that occurred
     */
    public static void logError(String operation, Throwable error) {
        try {
            logger.error("═══════════════════════════════════════════════");
            logger.error("❌ MCP ERROR: {}", operation);
            logger.error("───────────────────────────────────────────────");
            logger.error("Timestamp: {}", Instant.now());
            logger.error("Error Type: {}", error.getClass().getSimpleName());
            logger.error("Error Message: {}", error.getMessage());
            logger.error("═══════════════════════════════════════════════\n", error);

        } catch (Exception e) {
            logger.error("Failed to log error for {}", operation, e);
        }
    }

    /**
     * Set maximum response length for logging (to avoid huge log files)
     *
     * @param maxLength Maximum number of characters to log for responses
     */
    public static void setMaxResponseLength(int maxLength) {
        // This would require making MAX_RESPONSE_LENGTH non-final
        // For now, it's a constant
        logger.info("Note: MAX_RESPONSE_LENGTH is currently a constant ({}). Modify McpLogger.java to change it.", MAX_RESPONSE_LENGTH);
    }
}
