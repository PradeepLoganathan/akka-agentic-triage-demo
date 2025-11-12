# Knowledge Base MCP Server

Standalone MCP (Model Context Protocol) server providing access to service runbooks and documentation. This server simulates knowledge management systems like Confluence, SharePoint, Jira, and ServiceNow.

## Purpose

This service exposes MCP resources for accessing organizational knowledge:
- **Service Runbooks**: Troubleshooting guides with escalation procedures
- **Escalation Contacts**: Team contact information (**contains PII for demo**)
- **Historical Procedures**: Best practices and operational guidelines

**IMPORTANT**: This server intentionally contains PII (email addresses, phone numbers) in runbooks to demonstrate guardrail detection when accessed by KnowledgeBaseAgent.

## Port & Service Name

- **Port**: `9300`
- **Service Name**: `knowledge-base-mcp-server`
- **MCP Endpoint**: `http://localhost:9300/mcp`

## Architecture

```
┌─────────────────────────────────────┐
│  Triage Service (port 9100)         │
│                                     │
│  KnowledgeBaseAgent →               │
│    RemoteMcpResources.fromService(  │
│      "knowledge-base-mcp-server"    │
│    )                                │
└──────────────┬──────────────────────┘
               │
               │ Service Discovery
               │ (dev-mode)
               ↓
┌─────────────────────────────────────┐
│  Knowledge Base MCP Server          │
│  (port 9300)                        │
│                                     │
│  @McpEndpoint                       │
│  Resource:                          │
│  - kb://runbooks/{serviceName}      │
│    (Contains PII for guardrail      │
│     demonstration)                  │
└─────────────────────────────────────┘
```

## MCP Resources Provided

### `kb://runbooks/{serviceName}`
Get troubleshooting runbook for a specific service

**URI Template**: `kb://runbooks/{serviceName}`

**Parameters:**
- `serviceName` (path param) - Service name (e.g., "payment-service")

**Returns**: Markdown-formatted runbook with:
- Symptom descriptions
- Common causes
- Diagnosis steps
- Remediation procedures
- **Escalation contacts with PII** (email, phone)

**Available Runbooks:**
- `payment-service-runbook.md` ✅ Contains PII
- `auth-service-runbook.md`
- `checkout-service-runbook.md`
- `api-gateway-runbook.md`
- `order-service-runbook.md`
- And more...

**Example Access:**
```
Resource URI: kb://runbooks/payment-service
Returns: Full runbook markdown including escalation contacts
```

## PII Guardrail Demonstration

### What PII is Included?

The `payment-service-runbook.md` contains:

```markdown
## Escalation Contacts

### Primary On-Call (Payment Team)
- **Name**: Sarah Chen, Senior SRE
- **Email**: sarah.chen@company.com         ⚠️ PII - Email
- **Phone**: +1-555-123-4567                ⚠️ PII - Phone
- **Slack**: @sarah.chen

### Secondary On-Call (Database Team)
- **Email**: michael.rodriguez@company.com  ⚠️ PII - Email
- **Phone**: +1-555-987-6543                ⚠️ PII - Phone

### Team Distribution Lists
- payments-team@company.com                 ⚠️ PII - Email
- dba-team@company.com                      ⚠️ PII - Email
```

### Expected Behavior

When KnowledgeBaseAgent accesses `kb://runbooks/payment-service`:

1. **MCP Resource Call**: Agent requests runbook via MCP
2. **KB Server Returns**: Full runbook including PII section
3. **PII Guardrail Triggers**: Detects email addresses and phone numbers
4. **Agent Blocked**: KnowledgeBaseAgent fails with guardrail violation
5. **Workflow Continues**: TriageWorkflow proceeds without KB context (resilience demo)

**Log Output:**
```
🚨 PII DETECTED: email address from agent 'knowledge-base-agent' (likely from MCP tool/resource - KnowledgeBaseAgent uses MCP)
Guardrails blocked PII from being exposed.
```

## Running the Service

### Prerequisites
- Java 21+
- Maven 3.8+
- Port 9300 available

### Start Knowledge Base MCP Server

```bash
# Terminal 2: Knowledge Base MCP Server
cd knowledge-base-mcp-server
mvn compile exec:java
```

**Expected Output:**
```
INFO  akka.runtime.DiscoveryManager - Akka Runtime started at 127.0.0.1:9300
INFO  ... - MCP endpoint component [...KnowledgeBaseEndpoint], path [/mcp]
INFO  ... - Service name: knowledge-base-mcp-server
```

### Test the Server

```bash
# List available resources
curl -s http://localhost:9300/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"resources/list","params":{}}' \
  | python3 -m json.tool
```

## Full Demo Setup

### Start All Three Services

```bash
# Terminal 1: Evidence MCP Server
cd evidence-mcp-server
mvn compile exec:java
# → Started on http://localhost:9200

# Terminal 2: Knowledge Base MCP Server
cd ../knowledge-base-mcp-server
mvn compile exec:java
# → Started on http://localhost:9300

# Terminal 3: Agentic Triage System
cd ../agentic-triage-system
export OPENAI_API_KEY="your-key-here"
mvn compile exec:java
# → Started on http://localhost:9100
```

### Run Guardrail Demo

```bash
# Start a triage workflow for payment service
curl -X POST http://localhost:9100/triage/test-kb-pii-001 \
  -H "Content-Type: application/json" \
  -d '{
    "incident": "Payment service is down with 503 errors"
  }'

# Watch the logs - you'll see:
# 1. EvidenceAgent successfully gathers logs/metrics from port 9200
# 2. KnowledgeBaseAgent attempts to get runbook from port 9300
# 3. 🚨 PII Guardrail blocks the runbook (contains emails/phones)
# 4. Workflow continues and completes without KB context

# Check workflow state
curl http://localhost:9100/triage/test-kb-pii-001/state
```

## Service Discovery

**Knowledge Base MCP Server (`application.conf`):**
```hocon
akka.javasdk.dev-mode {
  service-name = "knowledge-base-mcp-server"
  http-port = 9300
}
```

**KnowledgeBaseAgent (`KnowledgeBaseAgent.java`):**
```java
.mcpResources(
    RemoteMcpResources.fromService("knowledge-base-mcp-server")
        .withAllowedResourceUriPatterns("kb://runbooks/*")
)
```

Akka automatically discovers the service running locally and routes MCP resource requests to port 9300.

## Mock Runbooks

Located in `src/main/resources/knowledge_base/`:

### Runbooks with Full Content
- `payment-service-runbook.md` - **Contains PII** for guardrail demo
- `auth-service-runbook.md` - Authentication troubleshooting
- `checkout-service-runbook.md` - Checkout process issues
- `api-gateway-runbook.md` - Gateway routing problems
- `order-service-runbook.md` - Order processing guides

### Additional Documentation
- `circuit-breaker-guidelines.md` - Circuit breaker patterns
- `database-operations.md` - Database maintenance procedures
- `post-deployment-checklist.md` - Deployment verification steps

## Testing Scenarios

### Scenario 1: PII Guardrail Trigger
**Objective**: Demonstrate PII detection from MCP resource

**Steps:**
1. Start all three services
2. Submit payment service incident
3. Observe KnowledgeBaseAgent blocked by guardrail
4. Verify workflow completes without KB context

**Expected Logs:**
```
INFO  KnowledgeBaseAgent - 📚 MCP Resource: getRunbook called - Service: payment-service
INFO  KnowledgeBaseAgent - 📚 Runbook loaded - Service: payment-service, Size: 2847 bytes
ERROR PiiGuardrail - 🚨 PII DETECTED: email address from agent 'knowledge-base-agent' (likely from MCP)
INFO  TriageWorkflow - ⚠️ Knowledge base search failed, continuing workflow
```

### Scenario 2: Successful Runbook Access (No PII)
**Objective**: Access runbook without PII

**Steps:**
1. Modify `auth-service-runbook.md` to ensure no PII
2. Submit auth service incident
3. Observe successful KB access

## Troubleshooting

### Service Not Found
**Symptom**: Triage service logs "Service not found: knowledge-base-mcp-server"

**Solutions:**
1. Verify KB MCP Server is running: `curl http://localhost:9300/mcp`
2. Check `service-name` in `application.conf` matches exactly
3. Ensure port 9300 is not blocked by firewall
4. Check for port conflicts: `lsof -i :9300`

### Resource Not Accessible
**Symptom**: Agent cannot access kb:// resources

**Check KnowledgeBaseAgent configuration:**
```java
// Must use RemoteMcpResources, not RemoteMcpTools
.mcpResources(
    RemoteMcpResources.fromService("knowledge-base-mcp-server")
        .withAllowedResourceUriPatterns("kb://runbooks/*")
)
```

### PII Guardrail Not Triggering
**Symptom**: Runbook with PII passes through guardrail

**Solutions:**
1. Verify PII patterns in runbook (email format, phone format)
2. Check guardrail is enabled on KnowledgeBaseAgent:
   ```java
   @Guardrails({PiiGuardrail.class, ...})
   ```
3. Review PII guardrail regex patterns in `PiiGuardrail.java`

### Port Already in Use
**Symptom**: `Address already in use: bind`

**Solutions:**
```bash
# Find process using port 9300
lsof -i :9300

# Kill the process
kill -9 <PID>

# Or change port in application.conf
akka.javasdk.dev-mode.http-port = 9301
```

## File Structure

```
knowledge-base-mcp-server/
├── pom.xml                                    # Maven configuration
├── README.md                                  # This file
├── src/main/
│   ├── java/com/pradeepl/knowledgebase/
│   │   ├── KnowledgeBaseEndpoint.java         # MCP endpoint with runbook resource
│   │   └── util/
│   │       └── McpLogger.java                 # MCP operation logging
│   └── resources/
│       ├── application.conf                   # Service configuration (port 9300)
│       └── knowledge_base/
│           ├── payment-service-runbook.md     # ⚠️ Contains PII
│           ├── auth-service-runbook.md
│           ├── checkout-service-runbook.md
│           ├── api-gateway-runbook.md
│           ├── order-service-runbook.md
│           ├── circuit-breaker-guidelines.md
│           ├── database-operations.md
│           ├── post-deployment-checklist.md
│           └── incidents/                     # Historical incident reports
└── target/                                    # Compiled classes
```

## Demo Talking Points

When demonstrating this system:

1. **Two-Server Architecture**:
   - "We have two MCP servers: Evidence (port 9200) for operational data, and Knowledge Base (port 9300) for documentation"

2. **Enterprise Realism**:
   - "This mirrors real enterprises where you have Datadog for metrics AND Confluence for runbooks - two separate systems"

3. **PII Guardrail**:
   - "The KB server returns a runbook with team email addresses"
   - "Watch the guardrail detect and block the PII before it reaches the LLM"
   - "Notice the workflow continues successfully - this shows resilience"

4. **MCP Integration**:
   - "Evidence Agent uses MCP Tools, KB Agent uses MCP Resources"
   - "This demonstrates the full MCP protocol - both tools and resources"

## Production Deployment

For production deployment considerations:

### Security
- Remove all PII from runbooks (or redact appropriately)
- Enable authentication (LDAP, OAuth, API keys)
- Implement role-based access control (RBAC)
- Add request validation and sanitization
- Enable audit logging for all access

### Integration
- Connect to real Confluence/SharePoint APIs
- Implement semantic search across documentation
- Add caching layer for frequently accessed runbooks
- Version control for runbook updates

### Monitoring
- Track resource access patterns
- Monitor PII detection rate (should be zero in prod)
- Alert on guardrail violations
- Measure response times

## Related Services

- **Evidence MCP Server** (port 9200): Operational data (logs, metrics)
- **Agentic Triage System** (port 9100): Main triage workflow orchestration

## Support

For issues or questions:
1. Check logs in both KB MCP Server and Triage Service
2. Verify PII is present in runbooks for demo purposes
3. Test MCP endpoint directly with curl
4. Review guardrail configuration in agents
