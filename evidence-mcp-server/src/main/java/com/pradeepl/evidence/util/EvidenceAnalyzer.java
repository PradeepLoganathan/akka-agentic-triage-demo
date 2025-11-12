package com.pradeepl.evidence.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared utility class for analyzing logs and metrics across MCP endpoints.
 * Provides common analysis functions for log parsing, metrics formatting, and pattern detection.
 */
public class EvidenceAnalyzer {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Record representing the results of log analysis
     */
    public record LogAnalysis(
        int errorCount,
        List<String> errorPatterns,
        Map<String, Integer> statusCodeCounts,
        List<String> anomalies,
        List<String> sampleErrorLines
    ) {}

    /**
     * Analyzes log content for errors, patterns, and anomalies.
     *
     * @param logs The raw log content to analyze
     * @return LogAnalysis object containing analysis results
     */
    public static LogAnalysis analyzeLogs(String logs) {
        if (logs == null || logs.isEmpty()) {
            return new LogAnalysis(0, List.of(), Map.of(), List.of(), List.of());
        }

        List<String> errorPatterns = new ArrayList<>();
        List<String> anomalies = new ArrayList<>();
        int errorCount = 0;
        Map<String, Integer> statusCounts = new HashMap<>();
        List<String> sampleErrorLines = new ArrayList<>();

        // Common error patterns
        Pattern errorPattern = Pattern.compile("(?i)(error|exception|failed|timeout|refused)");
        Pattern httpErrorPattern = Pattern.compile("(?i)(5\\d{2}|4\\d{2})");
        Pattern dbErrorPattern = Pattern.compile("(?i)(connection.*refused|deadlock|timeout.*database)");

        String[] lines = logs.split("\\n");
        for (String line : lines) {
            if (errorPattern.matcher(line).find()) {
                errorCount++;
                if (sampleErrorLines.size() < 5) {
                    sampleErrorLines.add(line.trim());
                }
            }

            Matcher httpMatcher = httpErrorPattern.matcher(line);
            if (httpMatcher.find()) {
                String code = httpMatcher.group(1);
                if (!errorPatterns.contains("HTTP " + code + " errors")) {
                    errorPatterns.add("HTTP " + code + " errors");
                }
                statusCounts.merge(code, 1, Integer::sum);
            }

            if (dbErrorPattern.matcher(line).find()) {
                if (!errorPatterns.contains("Database connectivity issues")) {
                    errorPatterns.add("Database connectivity issues");
                }
            }
        }

        // Detect anomalies
        if (errorCount > lines.length * 0.1) {
            anomalies.add(String.format("High error rate (%d errors in %d lines = %.1f%%)",
                errorCount, lines.length, (errorCount * 100.0 / lines.length)));
        }

        return new LogAnalysis(errorCount, errorPatterns, statusCounts, anomalies, sampleErrorLines);
    }

    /**
     * Determines which metrics file to use based on the query expression.
     *
     * @param expr The metrics query expression
     * @return The path to the appropriate metrics file
     */
    public static String determineMetricsFile(String expr) {
        String e = expr == null ? "" : expr.toLowerCase();

        // Service-specific routing
        if (e.contains("gateway") && (e.contains("error") || e.contains("5xx"))) {
            return "metrics/api-gateway-errors.json";
        }
        if (e.contains("checkout") && (e.contains("latency") || e.contains("p95") || e.contains("response_time"))) {
            return "metrics/checkout-service-latency.json";
        }
        if (e.contains("auth") && (e.contains("error") || e.contains("fail"))) {
            return "metrics/auth-service-errors.json";
        }
        if (e.contains("db") || e.contains("database")) {
            return "metrics/db-performance.json";
        }

        // Generic mappings
        if (e.contains("error") || e.contains("fail")) {
            return "metrics/payment-service-errors.json";
        } else if (e.contains("latency") || e.contains("response_time")) {
            return "metrics/payment-service-latency.json";
        } else if (e.contains("cpu") || e.contains("memory") || e.contains("resource")) {
            return "metrics/user-service-resources.json";
        } else if (e.contains("throughput") || e.contains("rate")) {
            return "metrics/order-service-throughput.json";
        } else {
            return "metrics/payment-service-errors.json";
        }
    }

    /**
     * Formats metrics data into a human-readable summary.
     *
     * @param metricsData The parsed metrics JSON data
     * @param expr The query expression
     * @param range The time range for the query
     * @return Formatted string summary of the metrics
     */
    public static String formatMetricsOutput(JsonNode metricsData, String expr, String range) {
        StringBuilder output = new StringBuilder();
        output.append("Metrics Data Summary:\n");

        JsonNode metrics = metricsData.get("metrics");
        if (metrics == null) {
            return "Invalid metrics file format";
        }

        // Format error metrics
        if (metrics.has("error_rate")) {
            JsonNode errorRate = metrics.get("error_rate");
            double current = errorRate.has("current") ? errorRate.get("current").asDouble() : 0.0;
            String status = errorRate.has("status") ? errorRate.get("status").asText() : "unknown";

            // Handle both previous_hour and previous_window field names
            double previous = 0.0;
            if (errorRate.has("previous_hour")) {
                previous = errorRate.get("previous_hour").asDouble();
            } else if (errorRate.has("previous_window")) {
                previous = errorRate.get("previous_window").asDouble();
            }

            output.append(String.format("- Error Rate: %.1f%% (%s), Previous: %.1f%%\n",
                current, status, previous));

            if (metrics.has("error_count")) {
                JsonNode errorCount = metrics.get("error_count");
                if (errorCount.has("total")) {
                    output.append(String.format("- Total Errors: %d requests\n",
                        errorCount.get("total").asInt()));
                }
            }

            if (metrics.has("error_spike")) {
                JsonNode spike = metrics.get("error_spike");
                if (spike.has("detected") && spike.get("detected").asBoolean()) {
                    output.append(String.format("- Spike Detected: %s (peak: %.1f%%, cause: %s)\n",
                        spike.has("time_window") ? spike.get("time_window").asText() : "unknown",
                        spike.has("peak_rate") ? spike.get("peak_rate").asDouble() : 0.0,
                        spike.has("primary_cause") ? spike.get("primary_cause").asText() : "unknown"));
                }
            }
        }

        // Format latency metrics
        if (metrics.has("latency_percentiles")) {
            JsonNode latency = metrics.get("latency_percentiles");
            output.append(String.format("- Latency P95: %dms, P99: %dms, P99.9: %dms\n",
                latency.has("p95") ? latency.get("p95").asInt() : 0,
                latency.has("p99") ? latency.get("p99").asInt() : 0,
                latency.has("p99.9") ? latency.get("p99.9").asInt() : 0));

            if (metrics.has("average_latency")) {
                JsonNode avgLatency = metrics.get("average_latency");
                output.append(String.format("- Average Latency: %dms (%s), Baseline: %dms\n",
                    avgLatency.has("current") ? avgLatency.get("current").asInt() : 0,
                    avgLatency.has("status") ? avgLatency.get("status").asText() : "unknown",
                    avgLatency.has("baseline") ? avgLatency.get("baseline").asInt() : 0));
            }
        }

        // Format resource metrics
        if (metrics.has("cpu_utilization")) {
            JsonNode cpu = metrics.get("cpu_utilization");
            output.append(String.format("- CPU Usage: %.1f%% (%s), Peak: %.1f%%\n",
                cpu.has("current") ? cpu.get("current").asDouble() : 0.0,
                cpu.has("status") ? cpu.get("status").asText() : "unknown",
                cpu.has("peak_15min") ? cpu.get("peak_15min").asDouble() : 0.0));

            if (metrics.has("memory_utilization")) {
                JsonNode memory = metrics.get("memory_utilization");
                output.append(String.format("- Memory: Heap %.1f%%, GC Pressure: %s\n",
                    memory.has("heap_used") ? memory.get("heap_used").asDouble() : 0.0,
                    memory.has("gc_pressure") ? memory.get("gc_pressure").asText() : "unknown"));
            }
        }

        // Format throughput metrics
        if (metrics.has("request_rate")) {
            JsonNode requestRate = metrics.get("request_rate");
            output.append(String.format("- Request Rate: %d req/sec, Peak: %d req/sec\n",
                requestRate.has("current") ? requestRate.get("current").asInt() : 0,
                requestRate.has("peak_1h") ? requestRate.get("peak_1h").asInt() : 0));

            if (metrics.has("success_rate")) {
                JsonNode successRate = metrics.get("success_rate");
                output.append(String.format("- Success Rate: %.1f%% (%s), Target: %.1f%%\n",
                    successRate.has("current") ? successRate.get("current").asDouble() : 0.0,
                    successRate.has("status") ? successRate.get("status").asText() : "unknown",
                    successRate.has("target") ? successRate.get("target").asDouble() : 0.0));
            }
        }

        // Add alerts
        if (metrics.has("alerts")) {
            JsonNode alerts = metrics.get("alerts");
            if (alerts.isArray() && alerts.size() > 0) {
                output.append("- Active Alerts: ");
                for (JsonNode alert : alerts) {
                    output.append(alert.asText()).append("; ");
                }
                output.append("\n");
            }
        }

        return output.toString();
    }

    /**
     * Analyzes metrics query and provides insights.
     *
     * @param metrics The raw metrics content
     * @param expr The query expression
     * @return List of analytical insights
     */
    public static List<String> analyzeMetrics(String metrics, String expr) {
        List<String> insights = new ArrayList<>();

        if (metrics == null || metrics.isEmpty()) {
            insights.add("No metrics data available");
            return insights;
        }

        // Simple heuristic analysis
        if (expr.contains("error")) {
            insights.add("Error rate metrics requested - indicates error investigation");
        }
        if (expr.contains("latency") || expr.contains("response_time")) {
            insights.add("Performance metrics requested - indicates latency investigation");
        }
        if (expr.contains("cpu") || expr.contains("memory")) {
            insights.add("Resource utilization metrics - indicates capacity investigation");
        }

        // Look for extreme values
        if (metrics.contains("100%") || metrics.contains("0.00")) {
            insights.add("Extreme values detected - potential system limits or failures");
        }

        return insights;
    }
}
