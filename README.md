# Akka Agentic AI Triage Demo

> Enterprise-grade multi-agent incident triage system demonstrating Akka Java SDK with MCP protocol integration

## 🎯 Overview

This project demonstrates the **art of possible with Akka for agentic AI** by showcasing a complete incident triage system powered by multiple AI agents coordinating through the Model Context Protocol (MCP).

### What This Demonstrates

- ✅ **Multi-Agent Coordination**: Evidence gathering, knowledge base access, and triage orchestration
- ✅ **MCP Protocol Integration**: Remote MCP tools (operational data) and resources (documentation)
- ✅ **Workflow Orchestration**: 5-step automated triage workflow with Akka Workflows
- ✅ **Guardrails**: PII detection across external systems to prevent data leakage
- ✅ **Service Discovery**: Automatic service discovery in Akka dev-mode
- ✅ **Resilience**: Graceful degradation when external systems fail
- ✅ **Event-Driven Architecture**: Akka Event Sourced Entities and message passing

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                   Agentic Triage System                         │
│                        (Port 9100)                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Evidence   │  │  Knowledge   │  │    Triage    │          │
│  │    Agent     │  │  Base Agent  │  │   Workflow   │          │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘          │
│         │                 │                                     │
│         │ MCP Tools       │ MCP Resources                       │
└─────────┼─────────────────┼─────────────────────────────────────┘
          │                 │
          ↓                 ↓
┌──────────────────┐  ┌──────────────────┐
│  Evidence MCP    │  │ Knowledge Base   │
│     Server       │  │   MCP Server     │
│  (Port 9200)     │  │  (Port 9300)     │
│                  │  │                  │
│  Tools:          │  │  Resources:      │
│  - fetch_logs    │  │  - runbooks      │
│  - query_metrics │  │  - docs          │
│  - get_services  │  │  (with PII)      │
│  - correlate     │  │                  │
└──────────────────┘  └──────────────────┘
```

### System Components

| Component | Port | Purpose | MCP Type | Technology |
|-----------|------|---------|----------|------------|
| **Evidence MCP Server** | 9200 | Operational data (logs, metrics) | Tools | Splunk/Datadog-like |
| **Knowledge Base MCP Server** | 9300 | Documentation & runbooks | Resources | Confluence/SharePoint-like |
| **Agentic Triage System** | 9100 | Multi-agent orchestration | Consumer | Akka Workflows |

## 🚀 Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- `OPENAI_API_KEY` environment variable set

### Build All Services

```bash
# Clone repository
git clone https://github.com/YourAccount/akka-agentic-triage-demo
cd akka-agentic-triage-demo

# Build entire monorepo (note: agentic-triage-system has known compilation issues)
mvn clean install -DskipTests

# Build individual modules
cd evidence-mcp-server && mvn compile
cd knowledge-base-mcp-server && mvn compile
```

### Run the Demo

**Terminal 1 - Evidence MCP Server:**
```bash
cd evidence-mcp-server
mvn compile exec:java

# Wait for: "Akka Runtime started at 127.0.0.1:9200"
```

**Terminal 2 - Knowledge Base MCP Server:**
```bash
cd knowledge-base-mcp-server
mvn compile exec:java

# Wait for: "Akka Runtime started at 127.0.0.1:9300"
```

**Terminal 3 - Agentic Triage System:**
```bash
cd agentic-triage-system
export OPENAI_API_KEY=your-key-here
mvn compile exec:java

# Wait for: "Akka Runtime started at 127.0.0.1:9100"
```

### Test the System

```bash
# Test Evidence MCP Server
curl http://localhost:9200/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}'

# Test Knowledge Base MCP Server
curl http://localhost:9300/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"resources/list","params":{}}'

# Start a triage workflow
curl -X POST http://localhost:9100/triage/demo-001 \
  -H "Content-Type: application/json" \
  -d '{"incident": "Payment service experiencing high error rates"}'

# Check workflow status
curl http://localhost:9100/triage/demo-001/state
```

## 📖 Documentation

### Service-Specific Documentation

- [Evidence MCP Server](evidence-mcp-server/README.md) - Operational data tools
- [Knowledge Base MCP Server](knowledge-base-mcp-server/README.md) - Documentation resources
- [Agentic Triage System](agentic-triage-system/README.md) - Main orchestration

### Key Concepts

#### MCP (Model Context Protocol)

This demo showcases both types of MCP endpoints:

- **Tools** (Evidence Server): Callable functions for active data gathering
- **Resources** (Knowledge Base Server): Retrievable content for context enrichment

#### Multi-Agent Workflow

The triage system orchestrates multiple agents:

1. **Classification Agent**: Categorizes incident severity and type
2. **Evidence Agent**: Gathers logs and metrics via MCP tools
3. **Knowledge Base Agent**: Retrieves runbooks via MCP resources
4. **Triage Agent**: Analyzes evidence and recommends actions
5. **Remediation Agent**: Proposes fixes based on runbooks

#### Guardrails

The system demonstrates PII detection guardrails:
- Knowledge Base runbooks contain team contact information (emails, phone numbers)
- PiiGuardrail intercepts and blocks PII before returning to agents
- Workflow continues gracefully despite blocked data

## 🎬 Demo Scenarios

### Scenario 1: Payment Service Outage (P1)

```bash
curl -X POST http://localhost:9100/triage/payment-outage-001 \
  -H "Content-Type: application/json" \
  -d '{
    "incident": "Payment service is down with 503 errors"
  }'
```

**Expected Flow:**
1. Evidence Agent fetches payment-service logs → 503 errors detected
2. Evidence Agent queries error metrics → spike at 14:25 UTC
3. Knowledge Base Agent retrieves payment-service runbook → **PII BLOCKED**
4. Triage Agent correlates evidence → database connection pool exhaustion
5. Remediation Agent recommends rollout restart

### Scenario 2: Database Performance Degradation (P2)

```bash
curl -X POST http://localhost:9100/triage/db-perf-001 \
  -H "Content-Type: application/json" \
  -d '{
    "incident": "Checkout service experiencing high latency"
  }'
```

**Expected Flow:**
1. Evidence Agent fetches checkout-service logs → database timeout errors
2. Evidence Agent queries database metrics → 85% CPU sustained
3. Knowledge Base Agent retrieves database operations guide
4. Triage Agent identifies resource exhaustion
5. Remediation Agent recommends scaling database

## 🔧 Akka Features Showcased

### Core Akka Java SDK Features

- **Agents**: AI-powered components with model provider integration
- **Workflows**: Multi-step business process orchestration
- **Views**: State projection for efficient querying
- **Event Sourced Entities**: Stateful components with event persistence
- **HTTP Endpoints**: REST API exposure with ACL annotations
- **Service Discovery**: Automatic discovery in dev-mode

### MCP Integration

- **@McpEndpoint**: Expose MCP protocol endpoints
- **@McpTool**: Define callable tools with descriptions
- **@McpResource**: Define retrievable resources with URI templates
- **RemoteMcpTools**: Consume tools from remote MCP servers
- **RemoteMcpResources**: Consume resources from remote MCP servers

### Advanced Patterns

- **Guardrails**: Agent output validation and filtering
- **Memory Provider**: Agent memory management
- **Model Provider**: LLM integration abstraction
- **Request Context**: Access to tracing, security, headers

## 📁 Repository Structure

```
akka-agentic-triage-demo/
├── pom.xml                           # Parent POM (multi-module)
├── README.md                         # This file
│
├── evidence-mcp-server/              # MCP Tools Server
│   ├── pom.xml
│   ├── README.md
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/pradeepl/evidence/
│   │       │       ├── EvidenceToolsEndpoint.java
│   │       │       └── util/
│   │       └── resources/
│   │           ├── logs/             # Mock log files
│   │           └── metrics/          # Mock metric files
│   └── requests.http                 # HTTP test requests
│
├── knowledge-base-mcp-server/        # MCP Resources Server
│   ├── pom.xml
│   ├── README.md
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/pradeepl/knowledgebase/
│   │       │       └── KnowledgeBaseEndpoint.java
│   │       └── resources/
│   │           └── knowledge_base/
│   │               ├── *-runbook.md  # Service runbooks (with PII)
│   │               └── incidents/    # Historical incidents
│   └── requests.http
│
└── agentic-triage-system/            # Main Orchestration
    ├── pom.xml
    ├── README.md
    ├── src/
    │   └── main/
    │       ├── java/
    │       │   └── com/pradeepl/triage/
    │       │       ├── application/
    │       │       │   ├── agents/   # AI Agents
    │       │       │   ├── workflows/ # Triage Workflows
    │       │       │   └── endpoints/ # HTTP APIs
    │       │       └── domain/       # Entities & Views
    │       └── resources/
    └── guard-requests.http
```

## 🛠️ Development

### Building Individual Modules

```bash
# Build Evidence MCP Server only
mvn -pl evidence-mcp-server clean compile

# Build Knowledge Base MCP Server only
mvn -pl knowledge-base-mcp-server clean compile

# Build Agentic Triage System only
mvn -pl agentic-triage-system clean compile
```

### Running Tests

```bash
# Run all tests
mvn test

# Test specific module
mvn -pl evidence-mcp-server test
```

## 🐛 Known Issues

- **Agentic Triage System**: Compilation error with `RemoteMcpResources` class - this feature may require a newer Akka SDK version or different implementation approach

## 📚 Learn More

- [Akka Java SDK Documentation](https://doc.akka.io/java-sdk/)
- [Model Context Protocol (MCP)](https://modelcontextprotocol.io/)
- [Akka Agents Guide](https://doc.akka.io/java-sdk/agents.html)
- [Akka Workflows Guide](https://doc.akka.io/java-sdk/workflows.html)

## 📄 License

This project is provided as a demonstration of Akka Java SDK capabilities.

## 🤝 Contributing

This is a demonstration project. Feel free to fork and experiment!

---

**Built with Akka Java SDK 3.5.6**
