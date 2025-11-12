# Evidence MCP Server

Standalone MCP (Model Context Protocol) server providing evidence gathering tools for incident triage. This server simulates operational data systems like Splunk, Datadog, Prometheus, and CloudWatch.

## Purpose

This service exposes MCP tools for collecting and analyzing operational evidence:
- **Logs**: Service log fetching with automatic error analysis
- **Metrics**: Performance metrics querying with insights
- **Service Catalog**: Available services list
- **Analysis**: Evidence correlation and pattern detection

## Port & Service Name

- **Port**: `9200`
- **Service Name**: `evidence-mcp-server`
- **MCP Endpoint**: `http://localhost:9200/mcp`

## Architecture

```
┌─────────────────────────────────────┐
│  Triage Service (port 9100)         │
│                                     │
│  EvidenceAgent →                    │
│    RemoteMcpTools.fromService(      │
│      "evidence-mcp-server"          │
│    )                                │
└──────────────┬──────────────────────┘
               │
               │ Service Discovery
               │ (dev-mode)
               ↓
┌─────────────────────────────────────┐
│  Evidence MCP Server (port 9200)    │
│                                     │
│  @McpEndpoint                       │
│  Tools:                             │
│  - fetch_logs                       │
│  - query_metrics                    │
│  - get_known_services               │
│  - correlate_evidence               │
└─────────────────────────────────────┘
```

## MCP Tools Provided

### 1. `fetch_logs`
Fetch service logs with automatic error analysis

**Arguments:**
- `service` (string) - Service name (e.g., "payment-service")
- `lines` (integer) - Number of log lines to fetch

**Returns:** JSON with logs, error count, patterns, anomalies, and sample errors

**Example:**
```json
{
  "service": "payment-service",
  "lines": 200
}
```

### 2. `query_metrics`
Query performance metrics with insights

**Arguments:**
- `expr` (string) - Metrics expression (e.g., "error_rate", "latency")
- `range` (string) - Time range (e.g., "1h", "30m")

**Returns:** JSON with raw metrics, formatted summary, and insights

**Example:**
```json
{
  "expr": "errors:rate5m",
  "range": "1h"
}
```

### 3. `get_known_services`
Get the complete list of available services

**Arguments:** None

**Returns:** Text list of services organized by categories

### 4. `correlate_evidence`
Correlate findings across logs and metrics

**Arguments:**
- `logFindings` (string) - Description of log findings
- `metricFindings` (string) - Description of metric findings

**Returns:** JSON with correlation analysis and confidence assessment

## Running the Service

### Prerequisites
- Java 21+
- Maven 3.8+
- Ports 9200 available

### Start Evidence MCP Server

```bash
# Terminal 1: Evidence MCP Server
cd evidence-mcp-server
mvn compile exec:java
```

**Expected Output:**
```
INFO  akka.runtime.DiscoveryManager - Akka Runtime started at 127.0.0.1:9200
INFO  ... - MCP endpoint component [...EvidenceToolsEndpoint], path [/mcp]
INFO  ... - Service name: evidence-mcp-server
```

### Test the Server

```bash
# List available tools
curl -s http://localhost:9200/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}' \
  | python3 -m json.tool
```

## Testing with Triage Service

### Start Both Services

```bash
# Terminal 1: Evidence MCP Server
cd evidence-mcp-server
mvn compile exec:java

# Terminal 2: Triage Service
cd ../agentic-triage-system
export OPENAI_API_KEY="your-key-here"
mvn compile exec:java
```

### Run a Workflow

```bash
# Start a triage workflow
curl -X POST http://localhost:9100/triage/test-evidence-001 \
  -H "Content-Type: application/json" \
  -d '{
    "incident": "Payment service experiencing high error rates"
  }'

# Check workflow status
curl http://localhost:9100/triage/test-evidence-001/state
```

## Service Discovery

In development mode, services discover each other automatically:

**Evidence MCP Server (`application.conf`):**
```hocon
akka.javasdk.dev-mode {
  service-name = "evidence-mcp-server"
  http-port = 9200
}
```

**EvidenceAgent (`EvidenceAgent.java`):**
```java
.mcpTools(
    RemoteMcpTools.fromService("evidence-mcp-server")
        .withAllowedToolNames("fetch_logs", "query_metrics", "correlate_evidence")
)
```

Akka automatically discovers the service running locally and routes MCP tool calls to port 9200.

## Mock Data

The server provides realistic mock data stored in `src/main/resources/`:

### Logs (`logs/`)
- `payment-service.log` - Payment service errors and transactions
- `auth-service.log` - Authentication failures and attempts
- `checkout-service.log` - Checkout process logs
- `api-gateway.log` - Gateway routing logs
- And more...

### Metrics (`metrics/`)
- `error-rate.json` - Error rate time series
- `latency.json` - Response time metrics
- `cpu-usage.json` - CPU utilization data
- `database-metrics.json` - Database performance
- And more...

### Service Catalog
- `services.json` - List of all available services

## Demo Scenarios

### Scenario 1: Payment Service Outage (P1)
**EvidenceAgent workflow:**
1. `fetch_logs("payment-service", 200)`
   - Returns: Logs with 503 Service Unavailable errors
   - Analysis: High error count, connection timeouts
2. `query_metrics("errors:rate5m", "1h")`
   - Returns: Spike in error rate starting at 14:25 UTC
   - Insight: Correlates with recent deployment
3. `correlate_evidence(...)`
   - Identifies: Deployment at 14:25, errors at 14:30
   - Confidence: High temporal correlation

### Scenario 2: Database Performance (P2)
**EvidenceAgent workflow:**
1. `fetch_logs("checkout-service", 200)`
   - Returns: Database timeout errors
2. `query_metrics("database_cpu", "1h")`
   - Returns: Sustained 85% CPU usage
3. `correlate_evidence(...)`
   - Identifies: Resource exhaustion pattern

## Troubleshooting

### Service Not Found
**Symptom**: Triage service logs "Service not found: evidence-mcp-server"

**Solutions:**
1. Verify Evidence MCP Server is running: `curl http://localhost:9200/mcp`
2. Check `service-name` in `application.conf` matches exactly
3. Ensure both services are in dev-mode
4. Check for port conflicts: `lsof -i :9200`

### MCP Tools Not Working
**Symptom**: Agent cannot call MCP tools

**Check logs for:**
```
Building component [com.pradeepl.evidence.EvidenceToolsEndpoint]
MCP endpoint component [...], path [/mcp]
```

**Solutions:**
1. Verify endpoint is registered properly
2. Test endpoint directly with curl
3. Check agent configuration references correct service name

### Connection Timeout
**Symptom**: MCP calls timeout after 60 seconds

**Solutions:**
1. Increase timeouts in `application.conf`:
   ```hocon
   akka.http.server {
     request-timeout = 120s
     idle-timeout = 240s
   }
   ```
2. Check resource availability (CPU, memory)
3. Monitor logs for slow operations

### Port Already in Use
**Symptom**: `Address already in use: bind`

**Solutions:**
```bash
# Find process using port 9200
lsof -i :9200

# Kill the process
kill -9 <PID>

# Or change port in application.conf
akka.javasdk.dev-mode.http-port = 9201
```

## File Structure

```
evidence-mcp-server/
├── pom.xml                              # Maven configuration
├── README.md                            # This file
├── src/main/
│   ├── java/com/pradeepl/evidence/
│   │   ├── EvidenceToolsEndpoint.java   # MCP endpoint with 4 tools
│   │   └── util/
│   │       ├── EvidenceAnalyzer.java    # Log/metrics analysis utilities
│   │       └── McpLogger.java           # MCP operation logging
│   └── resources/
│       ├── application.conf             # Service configuration (port 9200)
│       ├── logs/                        # Sample log files (16 services)
│       ├── metrics/                     # Sample metrics files (JSON)
│       └── services.json                # Service catalog
└── target/                              # Compiled classes
```

## Production Deployment

For production deployment considerations:

### Kubernetes
```yaml
apiVersion: v1
kind: Service
metadata:
  name: evidence-mcp-server
spec:
  ports:
  - port: 9200
    targetPort: 9200
  selector:
    app: evidence-mcp-server
```

### Service Discovery
- Use Kubernetes DNS: `http://evidence-mcp-server.namespace.svc.cluster.local:9200`
- Or configure explicit endpoints in Akka configuration

### Security
- Enable authentication (API keys, JWT)
- Implement rate limiting
- Enable TLS/HTTPS
- Add request validation and sanitization

### Monitoring
- Expose metrics endpoint
- Log all MCP tool calls
- Track response times and error rates
- Set up alerting for failures

## Related Services

- **Knowledge Base MCP Server** (port 9300): Runbooks and documentation with PII
- **Agentic Triage System** (port 9100): Main triage workflow orchestration

## Support

For issues or questions:
1. Check logs in both Evidence MCP Server and Triage Service
2. Verify network connectivity between services
3. Test MCP endpoint directly with curl
4. Review service discovery configuration
