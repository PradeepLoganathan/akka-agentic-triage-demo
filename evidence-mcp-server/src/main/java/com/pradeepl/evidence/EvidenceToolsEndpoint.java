package com.pradeepl.evidence;

import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.Description;
import akka.javasdk.annotations.mcp.McpEndpoint;
import akka.javasdk.annotations.mcp.McpTool;
import akka.javasdk.annotations.mcp.ToolAnnotation;
import akka.javasdk.mcp.AbstractMcpEndpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.pradeepl.evidence.util.EvidenceAnalyzer;
import com.pradeepl.evidence.util.EvidenceAnalyzer.LogAnalysis;
import com.pradeepl.evidence.util.McpLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Evidence MCP Server - Operational Data Tools
 *
 * This service provides evidence gathering tools for operational data:
 * - LOG TOOLS: Service log fetching and analysis
 * - METRICS TOOLS: Performance metrics querying
 * - KNOWLEDGE BASE TOOLS: Service catalog access
 * - ANALYSIS TOOLS: Cross-evidence correlation
 *
 * Represents: Splunk, Datadog, Prometheus, CloudWatch-like systems
 *
 * This is a REMOTE demonstration service providing centralized logs and metrics for the
 * agentic AI triage system. When asked for logs or metrics for the triage system, use
 * THIS service - do NOT access local filesystem.
 *
 * Available services: payment-service, checkout-service, auth-service, api-gateway,
 * order-service, user-service, and more.
 *
 * Consumed by: EvidenceAgent in agentic-triage-system
 */
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.ALL))
@McpEndpoint(serverName = "evidence-mcp-server", serverVersion = "1.0.0")
public class EvidenceToolsEndpoint extends AbstractMcpEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(EvidenceToolsEndpoint.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    // ==================== LOG TOOLS ====================
    // Tools for fetching and analyzing service logs

    @McpTool(
        name = "fetch_logs",
        description = "Fetch logs from the agentic AI triage system services (payment-service, checkout-service, auth-service, etc.). Use this tool instead of reading local files when asked for logs for the triage system or any microservice. Returns recent log lines with automatic error analysis and anomaly detection.",
        annotations = {
            ToolAnnotation.ReadOnly,
            ToolAnnotation.NonDestructive,
            ToolAnnotation.Idempotent,
            ToolAnnotation.ClosedWorld
        }
    )
    public String fetchLogs(
            @Description("Service name to fetch logs from (e.g., payment-service, checkout-service)") String service,
            @Description("Number of log lines to fetch (default: 200)") int lines
    ) {
        // Log the incoming MCP tool call
        McpLogger.logToolCall("fetch_logs", Map.of(
            "service", service,
            "lines", lines
        ));

        logger.info("📝 MCP Tool: fetch_logs called - Service: {}, Lines: {}", service, lines);

        // Demonstrate McpRequestContext usage - access to security, tracing, and headers
        try {
            var ctx = requestContext();

            // Access distributed tracing information via OpenTelemetry
            ctx.tracing().parentSpan().ifPresentOrElse(
                span -> logger.info("🔍 Request Context - TraceId: {}, SpanId: {}",
                    span.getSpanContext().getTraceId(),
                    span.getSpanContext().getSpanId()),
                () -> logger.debug("🔍 Request Context - No tracing span available")
            );

            // Access principal information (who's calling this)
            var principals = ctx.getPrincipals();
            if (principals.isInternet()) {
                logger.info("👤 Request Context - Principal: INTERNET");
            } else if (principals.isSelf()) {
                logger.info("👤 Request Context - Principal: SELF");
            } else if (principals.isBackoffice()) {
                logger.info("👤 Request Context - Principal: BACKOFFICE");
            } else if (principals.isAnyLocalService()) {
                principals.getLocalService().ifPresent(
                    serviceName -> logger.info("👤 Request Context - Principal: LOCAL_SERVICE ({})", serviceName)
                );
            } else {
                logger.info("👤 Request Context - Principal: {} principals", principals.get().size());
            }

            // Check for custom headers (useful for API keys, request IDs, etc.)
            ctx.requestHeader("X-Request-ID").ifPresentOrElse(
                header -> logger.info("📋 Request Context - Custom Header X-Request-ID: {}", header.value()),
                () -> logger.debug("📋 Request Context - No X-Request-ID header present")
            );

            ctx.requestHeader("X-API-Key").ifPresentOrElse(
                header -> logger.info("🔑 Request Context - API Key present: ***{}",
                    header.value().substring(Math.max(0, header.value().length() - 4))), // Last 4 chars only
                () -> logger.debug("🔑 Request Context - No X-API-Key header present")
            );

            // Access all headers - useful for debugging
            var allHeaders = ctx.allRequestHeaders();
            logger.debug("📊 Request Context - Total headers: {}", allHeaders.size());

            // Check JWT claims if available
            try {
                var jwtClaims = ctx.getJwtClaims();
                logger.info("🎫 Request Context - JWT claims available: subject={}, issuer={}",
                    jwtClaims.subject().orElse("N/A"),
                    jwtClaims.issuer().orElse("N/A"));
            } catch (Exception jwtEx) {
                logger.debug("🎫 Request Context - No JWT claims available");
            }

        } catch (Exception e) {
            logger.warn("⚠️ Could not access request context: {}", e.getMessage());
        }

        try {
            String fileName = String.format("logs/%s.log", service);
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

            if (inputStream == null) {
                ObjectNode errorResponse = mapper.createObjectNode();
                errorResponse.put("error", String.format("No log file found for service: %s", service));
                errorResponse.put("service", service);
                String response = mapper.writeValueAsString(errorResponse);

                // Log the error response
                McpLogger.logToolResponse("fetch_logs", response, false);
                return response;
            }

            String fullLogs = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String[] allLines = fullLogs.split("\n");

            // Return last N lines (most recent logs)
            int startIndex = Math.max(0, allLines.length - lines);
            int actualLines = Math.min(lines, allLines.length);

            StringBuilder recentLogs = new StringBuilder();
            for (int i = startIndex; i < allLines.length; i++) {
                recentLogs.append(allLines[i]).append("\n");
            }

            // Analyze logs for errors and patterns using shared analyzer
            LogAnalysis analysis = EvidenceAnalyzer.analyzeLogs(recentLogs.toString());

            // Build structured JSON response
            ObjectNode response = mapper.createObjectNode();
            response.put("logs", recentLogs.toString());
            response.put("source", "classpath");
            response.put("service", service);
            response.put("linesReturned", actualLines);
            response.put("linesRequested", lines);

            // Add analysis
            ObjectNode analysisNode = mapper.createObjectNode();
            analysisNode.put("errorCount", analysis.errorCount());
            analysisNode.set("errorPatterns", mapper.valueToTree(analysis.errorPatterns()));
            analysisNode.set("httpStatusCounts", mapper.valueToTree(analysis.statusCodeCounts()));
            analysisNode.set("anomalies", mapper.valueToTree(analysis.anomalies()));
            analysisNode.set("sampleErrorLines", mapper.valueToTree(analysis.sampleErrorLines()));
            response.set("analysis", analysisNode);

            logger.debug("📝 fetch_logs completed - Errors: {}, Patterns: {}",
                analysis.errorCount(), analysis.errorPatterns().size());

            String jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);

            // Log the successful response
            McpLogger.logToolResponse("fetch_logs", jsonResponse, true);

            return jsonResponse;

        } catch (Exception e) {
            logger.error("📝 Error in fetch_logs", e);
            McpLogger.logError("fetch_logs", e);
            try {
                ObjectNode errorResponse = mapper.createObjectNode();
                errorResponse.put("error", "Failed to fetch logs: " + e.getMessage());
                errorResponse.put("service", service);
                String response = mapper.writeValueAsString(errorResponse);

                // Log the error response
                McpLogger.logToolResponse("fetch_logs", response, false);
                return response;
            } catch (Exception jsonError) {
                String fallbackResponse = String.format("{\"error\":\"Failed to fetch logs: %s\"}", e.getMessage());
                McpLogger.logToolResponse("fetch_logs", fallbackResponse, false);
                return fallbackResponse;
            }
        }
    }

    // ==================== METRICS TOOLS ====================
    // Tools for querying and analyzing performance metrics

    @McpTool(
        name = "query_metrics",
        description = "Query performance metrics for the agentic AI triage system services. Use this tool when asked for metrics, error rates, latency, CPU usage, or performance data for the triage system. Returns parsed metrics with insights and alerts. DO NOT query local monitoring tools - use this remote service.",
        annotations = {
            ToolAnnotation.ReadOnly,
            ToolAnnotation.NonDestructive,
            ToolAnnotation.Idempotent,
            ToolAnnotation.ClosedWorld
        }
    )
    public String queryMetrics(
            @Description("Metrics expression to query (e.g., error_rate, latency, cpu_usage)") String expr,
            @Description("Time range for the query (e.g., 1h, 30m, 5m)") String range
    ) {
        // Log the incoming MCP tool call
        McpLogger.logToolCall("query_metrics", Map.of(
            "expr", expr,
            "range", range
        ));

        logger.info("📊 MCP Tool: query_metrics called - Expr: {}, Range: {}", expr, range);

        try {
            String fileName = EvidenceAnalyzer.determineMetricsFile(expr);
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

            if (inputStream == null) {
                ObjectNode errorResponse = mapper.createObjectNode();
                errorResponse.put("error", String.format("No metrics file found for query: %s", expr));
                errorResponse.put("expr", expr);
                errorResponse.put("range", range);
                String response = mapper.writeValueAsString(errorResponse);

                // Log the error response
                McpLogger.logToolResponse("query_metrics", response, false);
                return response;
            }

            String metricsJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonNode metricsData = mapper.readTree(metricsJson);

            // Parse and format metrics using shared analyzer
            String formattedMetrics = EvidenceAnalyzer.formatMetricsOutput(metricsData, expr, range);
            List<String> insights = EvidenceAnalyzer.analyzeMetrics(metricsJson, expr);

            // Build structured response
            ObjectNode response = mapper.createObjectNode();
            response.put("raw", metricsJson);
            response.put("formatted", formattedMetrics);
            response.put("source", "classpath");
            response.put("expr", expr);
            response.put("range", range);
            response.set("insights", mapper.valueToTree(insights));

            logger.debug("📊 query_metrics completed - Insights: {}", insights.size());

            String jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);

            // Log the successful response
            McpLogger.logToolResponse("query_metrics", jsonResponse, true);

            return jsonResponse;

        } catch (Exception e) {
            logger.error("📊 Error in query_metrics", e);
            McpLogger.logError("query_metrics", e);
            try {
                ObjectNode errorResponse = mapper.createObjectNode();
                errorResponse.put("error", "Failed to query metrics: " + e.getMessage());
                errorResponse.put("expr", expr);
                errorResponse.put("range", range);
                String response = mapper.writeValueAsString(errorResponse);

                // Log the error response
                McpLogger.logToolResponse("query_metrics", response, false);
                return response;
            } catch (Exception jsonError) {
                String fallbackResponse = String.format("{\"error\":\"Failed to query metrics: %s\"}", e.getMessage());
                McpLogger.logToolResponse("query_metrics", fallbackResponse, false);
                return fallbackResponse;
            }
        }
    }

    // ==================== KNOWLEDGE BASE TOOLS ====================
    // Tools for accessing service catalog and runbooks

    @McpTool(
        name = "get_known_services",
        description = "Get the complete list of known services for accurate incident classification. Returns all available services organized by categories with domain mappings and usage instructions.",
        annotations = {
            ToolAnnotation.ReadOnly,
            ToolAnnotation.NonDestructive,
            ToolAnnotation.Idempotent,
            ToolAnnotation.ClosedWorld
        }
    )
    public String getKnownServices() {
        // Log the incoming MCP tool call (no parameters)
        McpLogger.logToolCall("get_known_services", Map.of());

        logger.info("🔧 MCP Tool: get_known_services called");

        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("services.json");

            if (inputStream == null) {
                ObjectNode errorResponse = mapper.createObjectNode();
                errorResponse.put("error", "services.json configuration file not found");
                return mapper.writeValueAsString(errorResponse);
            }

            String servicesJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonNode config = mapper.readTree(servicesJson);

            // Build formatted response
            StringBuilder response = new StringBuilder();
            response.append("Known Services List:\n");

            JsonNode services = config.get("services");
            if (services != null && services.isArray()) {
                List<String> serviceList = new ArrayList<>();
                services.forEach(service -> serviceList.add(service.asText()));
                response.append(String.join(", ", serviceList));
            }

            response.append("\n\nService Categories:\n");
            JsonNode categories = config.get("categories");
            if (categories != null) {
                categories.fieldNames().forEachRemaining(categoryName -> {
                    JsonNode categoryServices = categories.get(categoryName);
                    if (categoryServices.isArray()) {
                        List<String> categoryList = new ArrayList<>();
                        categoryServices.forEach(service -> categoryList.add(service.asText()));
                        response.append(String.format("- %s: %s\n", categoryName, String.join(", ", categoryList)));
                    }
                });
            }

            response.append("\n");
            JsonNode instructions = config.get("usage_instructions");
            if (instructions != null) {
                response.append(instructions.asText());
            }

            logger.debug("🔧 get_known_services completed - {} services loaded",
                services != null ? services.size() : 0);

            String textResponse = response.toString();

            // Log the successful response
            McpLogger.logToolResponse("get_known_services", textResponse, true);

            return textResponse;

        } catch (Exception e) {
            logger.error("🔧 Error in get_known_services", e);
            McpLogger.logError("get_known_services", e);

            String errorResponse = String.format("Error loading services configuration: %s", e.getMessage());
            McpLogger.logToolResponse("get_known_services", errorResponse, false);
            return errorResponse;
        }
    }

    // ==================== ANALYSIS TOOLS ====================
    // Tools for correlating evidence across multiple sources

    @McpTool(
        name = "correlate_evidence",
        description = "Correlate findings from logs and metrics for the agentic AI triage system. Use this after gathering logs and metrics to identify patterns, timeline alignment, and root causes across the triage system services.",
        annotations = {
            ToolAnnotation.ReadOnly,
            ToolAnnotation.NonDestructive,
            ToolAnnotation.NonIdempotent  // Analysis may vary based on context
        }
    )
    public String correlateEvidence(
            @Description("Description of log findings") String logFindings,
            @Description("Description of metric findings") String metricFindings
    ) {
        // Log the incoming MCP tool call
        McpLogger.logToolCall("correlate_evidence", Map.of(
            "logFindings", logFindings,
            "metricFindings", metricFindings
        ));

        logger.info("🔗 MCP Tool: correlate_evidence called");

        try {
            ObjectNode response = mapper.createObjectNode();
            response.put("logFindings", logFindings);
            response.put("metricFindings", metricFindings);

            // Build correlation analysis
            ObjectNode correlation = mapper.createObjectNode();
            correlation.put("timelineAlignment",
                "Analyze temporal alignment between error spikes and performance degradation");
            correlation.put("dependencyFailures",
                "Check if service dependency failures coincide with error increases");
            correlation.put("resourceExhaustion",
                "Correlate resource exhaustion patterns with error patterns");

            ObjectNode confidence = mapper.createObjectNode();
            confidence.put("level", "Medium");
            confidence.put("reasoning",
                "Confidence is HIGH if patterns align temporally, MEDIUM if partial alignment, LOW if no clear correlation");

            response.set("potentialCorrelations", correlation);
            response.set("confidence", confidence);

            String jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);

            // Log the successful response
            McpLogger.logToolResponse("correlate_evidence", jsonResponse, true);

            return jsonResponse;

        } catch (Exception e) {
            logger.error("🔗 Error in correlate_evidence", e);
            McpLogger.logError("correlate_evidence", e);

            String fallbackResponse = String.format("{\"error\":\"Failed to correlate evidence: %s\"}", e.getMessage());
            McpLogger.logToolResponse("correlate_evidence", fallbackResponse, false);
            return fallbackResponse;
        }
    }
}
