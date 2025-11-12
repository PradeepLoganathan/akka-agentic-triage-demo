package com.pradeepl.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EvidenceToolsEndpoint - Comprehensive MCP Tools Test Suite")
public class EvidenceToolsEndpointTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final EvidenceToolsEndpoint endpoint = new EvidenceToolsEndpoint();

    // ==================== LOG TOOLS TESTS ====================

    @Test
    @DisplayName("[LOGS] Should fetch and analyze payment service logs")
    public void testPaymentServiceLogs() throws Exception {
        String result = endpoint.fetchLogs("payment-service", 10);

        System.out.println("=== PAYMENT SERVICE LOGS ===");
        System.out.println(result);
        System.out.println();

        JsonNode response = mapper.readTree(result);

        assertThat(response.has("logs")).isTrue();
        assertThat(response.has("service")).isTrue();
        assertThat(response.has("analysis")).isTrue();
        assertThat(response.get("service").asText()).isEqualTo("payment-service");

        JsonNode analysis = response.get("analysis");
        assertThat(analysis.has("errorCount")).isTrue();
        assertThat(analysis.has("errorPatterns")).isTrue();
        assertThat(analysis.has("sampleErrorLines")).isTrue();

        String logs = response.get("logs").asText();
        assertThat(logs).isNotEmpty();
    }

    @Test
    @DisplayName("[LOGS] Should fetch and analyze user service logs")
    public void testUserServiceLogs() throws Exception {
        String result = endpoint.fetchLogs("user-service", 15);

        System.out.println("=== USER SERVICE LOGS ===");
        System.out.println(result);
        System.out.println();

        JsonNode response = mapper.readTree(result);

        assertThat(response.get("service").asText()).isEqualTo("user-service");
        assertThat(response.get("linesReturned").asInt()).isLessThanOrEqualTo(15);

        JsonNode analysis = response.get("analysis");
        assertThat(analysis).isNotNull();
    }

    @Test
    @DisplayName("[LOGS] Should analyze checkout service logs and detect patterns")
    public void testCheckoutServiceLogAnalysis() throws Exception {
        String result = endpoint.fetchLogs("checkout-service", 20);

        System.out.println("=== CHECKOUT SERVICE LOG ANALYSIS ===");
        System.out.println(result);
        System.out.println();

        JsonNode response = mapper.readTree(result);
        assertThat(response.get("service").asText()).isEqualTo("checkout-service");

        JsonNode analysis = response.get("analysis");
        assertThat(analysis.has("errorCount")).isTrue();
        assertThat(analysis.has("errorPatterns")).isTrue();
    }

    @Test
    @DisplayName("[LOGS] Should handle non-existent service gracefully")
    public void testNonExistentService() throws Exception {
        String result = endpoint.fetchLogs("non-existent-service", 5);

        System.out.println("=== NON-EXISTENT SERVICE TEST ===");
        System.out.println(result);
        System.out.println();

        JsonNode response = mapper.readTree(result);
        assertThat(response.has("error")).isTrue();
        assertThat(response.get("error").asText()).contains("No log file found for service: non-existent-service");
    }

    // ==================== METRICS TOOLS TESTS ====================

    @Test
    @DisplayName("[METRICS] Should query error metrics")
    public void testErrorMetrics() throws Exception {
        String result = endpoint.queryMetrics("error_rate", "1h");

        System.out.println("=== ERROR METRICS ===");
        System.out.println(result);
        System.out.println();

        JsonNode response = mapper.readTree(result);

        assertThat(response.has("formatted")).isTrue();
        assertThat(response.has("insights")).isTrue();
        assertThat(response.get("expr").asText()).isEqualTo("error_rate");

        String formatted = response.get("formatted").asText();
        assertThat(formatted).containsAnyOf("Error Rate", "Total Errors");
    }

    @Test
    @DisplayName("[METRICS] Should query latency metrics")
    public void testLatencyMetrics() throws Exception {
        String result = endpoint.queryMetrics("latency", "30m");

        System.out.println("=== LATENCY METRICS ===");
        System.out.println(result);
        System.out.println();

        JsonNode response = mapper.readTree(result);

        assertThat(response.has("formatted")).isTrue();
        String formatted = response.get("formatted").asText();
        assertThat(formatted).containsAnyOf("Latency P95", "Average Latency");
    }

    @Test
    @DisplayName("[METRICS] Should query resource metrics")
    public void testResourceMetrics() throws Exception {
        String result = endpoint.queryMetrics("cpu_usage", "15m");

        System.out.println("=== RESOURCE METRICS ===");
        System.out.println(result);
        System.out.println();

        JsonNode response = mapper.readTree(result);

        String formatted = response.get("formatted").asText();
        assertThat(formatted).containsAnyOf("CPU Usage", "Memory");
    }

    @Test
    @DisplayName("[METRICS] Should query throughput metrics")
    public void testThroughputMetrics() throws Exception {
        String result = endpoint.queryMetrics("throughput", "1h");

        System.out.println("=== THROUGHPUT METRICS ===");
        System.out.println(result);
        System.out.println();

        JsonNode response = mapper.readTree(result);

        String formatted = response.get("formatted").asText();
        assertThat(formatted).containsAnyOf("Request Rate", "Success Rate");
    }

    @Test
    @DisplayName("[METRICS] Should include insights in metrics response")
    public void testMetricsInsights() throws Exception {
        String result = endpoint.queryMetrics("error_rate", "1h");

        JsonNode response = mapper.readTree(result);
        JsonNode insights = response.get("insights");

        assertThat(insights).isNotNull();
        assertThat(insights.isArray()).isTrue();
        assertThat(insights.size()).isGreaterThan(0);
    }

    // ==================== KNOWLEDGE BASE TOOLS TESTS ====================

    @Test
    @DisplayName("[KNOWLEDGE] Should get list of known services")
    public void testGetKnownServices() {
        String result = endpoint.getKnownServices();

        System.out.println("=== KNOWN SERVICES ===");
        System.out.println(result);
        System.out.println();

        assertThat(result).contains("payment-service");
        assertThat(result).contains("checkout-service");
        assertThat(result).contains("auth-service");
        assertThat(result).contains("Service Categories");
    }

    @Test
    @DisplayName("[KNOWLEDGE] Should fetch payment service runbook")
    public void testPaymentServiceRunbook() {
        String result = endpoint.getRunbook("payment-service");

        System.out.println("=== PAYMENT SERVICE RUNBOOK ===");
        System.out.println(result);
        System.out.println();

        assertThat(result).contains("#");
        assertThat(result).doesNotContain("Runbook Not Found");
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("[KNOWLEDGE] Should fetch checkout service runbook")
    public void testCheckoutServiceRunbook() {
        String result = endpoint.getRunbook("checkout-service");

        System.out.println("=== CHECKOUT SERVICE RUNBOOK ===");
        System.out.println(result);
        System.out.println();

        assertThat(result).contains("#");
        assertThat(result).doesNotContain("Runbook Not Found");
    }

    @Test
    @DisplayName("[KNOWLEDGE] Should fetch auth service runbook")
    public void testAuthServiceRunbook() {
        String result = endpoint.getRunbook("auth-service");

        System.out.println("=== AUTH SERVICE RUNBOOK ===");
        System.out.println(result);
        System.out.println();

        assertThat(result).contains("#");
        assertThat(result).doesNotContain("Runbook Not Found");
    }

    @Test
    @DisplayName("[KNOWLEDGE] Should handle non-existent runbook gracefully")
    public void testNonExistentRunbook() {
        String result = endpoint.getRunbook("non-existent-service");

        System.out.println("=== NON-EXISTENT RUNBOOK TEST ===");
        System.out.println(result);
        System.out.println();

        assertThat(result).contains("Runbook Not Found");
        assertThat(result).contains("non-existent-service");
    }

    // ==================== ANALYSIS TOOLS TESTS ====================

    @Test
    @DisplayName("[ANALYSIS] Should correlate evidence from logs and metrics")
    public void testCorrelateEvidence() throws Exception {
        String result = endpoint.correlateEvidence(
            "High rate of HTTP 503 errors and database timeouts",
            "CPU spike to 95% at 14:30 UTC, memory at 87%"
        );

        System.out.println("=== CORRELATE EVIDENCE ===");
        System.out.println(result);
        System.out.println();

        JsonNode response = mapper.readTree(result);

        assertThat(response.has("logFindings")).isTrue();
        assertThat(response.has("metricFindings")).isTrue();
        assertThat(response.has("potentialCorrelations")).isTrue();
        assertThat(response.has("confidence")).isTrue();

        assertThat(response.get("logFindings").asText()).contains("HTTP 503 errors");
        assertThat(response.get("metricFindings").asText()).contains("CPU spike");
    }

    @Test
    @DisplayName("[ANALYSIS] Should provide correlation analysis")
    public void testCorrelationAnalysis() throws Exception {
        String result = endpoint.correlateEvidence(
            "Payment service errors increased from 0.5% to 12% at 15:00 UTC",
            "Database connection pool exhaustion detected at 14:58 UTC"
        );

        System.out.println("=== CORRELATION ANALYSIS ===");
        System.out.println(result);
        System.out.println();

        JsonNode response = mapper.readTree(result);
        JsonNode correlations = response.get("potentialCorrelations");

        assertThat(correlations).isNotNull();
        assertThat(correlations.has("timelineAlignment")).isTrue();
        assertThat(correlations.has("dependencyFailures")).isTrue();
        assertThat(correlations.has("resourceExhaustion")).isTrue();
    }

    @Test
    @DisplayName("[ANALYSIS] Should include confidence assessment")
    public void testConfidenceAssessment() throws Exception {
        String result = endpoint.correlateEvidence(
            "Authentication failures spiking",
            "Network latency increase detected"
        );

        System.out.println("=== CONFIDENCE ASSESSMENT ===");
        System.out.println(result);
        System.out.println();

        JsonNode response = mapper.readTree(result);
        JsonNode confidence = response.get("confidence");

        assertThat(confidence).isNotNull();
        assertThat(confidence.has("level")).isTrue();
        assertThat(confidence.has("reasoning")).isTrue();
    }
}
