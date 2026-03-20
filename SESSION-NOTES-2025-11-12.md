# Session Notes - November 12, 2025

## Session Summary
Fixed workflow error handling to gracefully continue when PII guardrails block knowledge base access.

## Problem Statement
The triage workflow was stopping at Step 4 (Knowledge Base Search) when the PII guardrail detected and blocked email addresses in service runbooks. The workflow failed with:

```
kalix.runtime.CorrelatedRuntimeException: Request tool result guardrail blocked, category [PII],
name [pii-detector]: Email address detected from agent 'pii-detector'.
Guardrails blocked PII from being exposed.
```

## Solution Implemented

### File Modified
`agentic-triage-system/src/main/java/com/pradeepl/triage/application/TriageWorkflow.java`

### Changes Made

#### 1. Added Step Recovery Configuration (line 170)
Added failover strategy for knowledge base step:
```java
.stepRecovery(TriageWorkflow::queryKnowledgeBaseStep,
              maxRetries(0).failoverTo(TriageWorkflow::knowledgeBaseFailoverStep))
```

#### 2. Created Knowledge Base Failover Step (lines 281-295)
New step that handles PII guardrail failures gracefully:
```java
@StepName("knowledge_base_failover")
private StepEffect knowledgeBaseFailoverStep() {
    logger.warn("⚠️ KNOWLEDGE BASE ACCESS BLOCKED - Continuing with remediation (likely due to PII guardrails)");
    String fallbackMessage = "# Knowledge Base Access Unavailable\n\n" +
            "Access to service runbooks was blocked by security guardrails (PII detection). " +
            "Proceeding with remediation based on available evidence and classification data only.";
    String conversationEntry = String.format("[%s] Knowledge base access blocked by security policy - continuing with available context",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
    return stepEffects()
            .updateState(currentState()
                    .withKnowledgeBaseResult(fallbackMessage)
                    .addConversation(new Conversation("system", conversationEntry))
                    .withStatus(TriageState.Status.KNOWLEDGE_BASE_SEARCHED))
            .thenTransitionTo(TriageWorkflow::remediateStep);
}
```

## How It Works Now

**Workflow Resilience Pattern:**
1. Step 4 (query_knowledge_base) attempts to access service runbooks via KnowledgeBaseAgent
2. If PII guardrail blocks the call → exception is thrown
3. Workflow automatically fails over to knowledge_base_failover step (no retries)
4. Failover step:
   - Logs a warning about the block
   - Sets a meaningful fallback message explaining the security policy enforcement
   - Updates workflow state with KNOWLEDGE_BASE_SEARCHED status
   - Continues to Step 5 (remediation)
5. Workflow completes all 7 steps successfully using available evidence and classification data

## What This Demonstrates

✅ **Workflow Resilience**: System gracefully handles security policy enforcement
✅ **PII Guardrails Working**: Successfully detects and blocks PII (emails in runbooks)
✅ **Graceful Degradation**: Workflow continues with reduced context rather than failing
✅ **Observability**: Clear logging shows why knowledge base was unavailable

## Testing Status

✅ **Compilation**: Successful (`mvn clean compile`)
⏳ **Runtime Testing**: Needs to be tested with all 3 services running

## Next Steps

1. **Test the Fix**:
   ```bash
   # Terminal 1 - Evidence MCP Server
   cd evidence-mcp-server && mvn compile exec:java

   # Terminal 2 - Knowledge Base MCP Server
   cd knowledge-base-mcp-server && mvn compile exec:java

   # Terminal 3 - Agentic Triage System (restart with new code)
   cd agentic-triage-system && mvn compile exec:java
   ```

2. **Trigger Workflow**:
   ```bash
   curl -X POST http://localhost:9100/triage/TRIAGE-TEST-001/start \
     -H "Content-Type: application/json" \
     -d '{"incident": "Payment service experiencing high error rates and timeouts"}'
   ```

3. **Verify Behavior**:
   - Workflow should reach Step 7 (finalize) and complete successfully
   - Step 4 should fail over to knowledge_base_failover
   - Logs should show: `⚠️ KNOWLEDGE BASE ACCESS BLOCKED - Continuing with remediation`
   - Final state should show fallback message in knowledgeBaseResult field

4. **Commit Changes**:
   ```bash
   git add agentic-triage-system/src/main/java/com/pradeepl/triage/application/TriageWorkflow.java
   git commit -m "Add workflow resilience for PII guardrail blocks

   - Added step recovery for queryKnowledgeBaseStep
   - Created knowledgeBaseFailoverStep to handle guardrail exceptions
   - Workflow now continues to remediation with fallback message
   - Demonstrates graceful degradation when security policies block access"
   ```

## Architecture Overview

### Three Services Running:
- **evidence-mcp-server** (port 9200): Provides logs, metrics, known services
- **knowledge-base-mcp-server** (port 9300): Provides runbooks (contains PII)
- **agentic-triage-system** (port 9100): Orchestrates 7-step triage workflow

### Workflow Steps:
1. **classify**: Identify service, severity, domain
2. **gather_evidence**: Collect logs and metrics
3. **triage**: Analyze root cause
4. **query_knowledge_base**: Access runbooks (🚨 PII guardrails apply here)
5. **remediate**: Generate remediation plan
6. **summarize**: Create stakeholder summaries
7. **finalize**: Complete workflow

### PII Guardrail:
- **Location**: `agentic-triage-system/src/main/java/com/pradeepl/triage/guardrails/PiiGuardrail.java`
- **Detection**: Uses `pii-detector` agent to scan MCP tool responses
- **Action**: Blocks responses containing emails, phone numbers, SSNs, credit cards
- **Demo Data**: Runbooks contain team emails like `payments-oncall@company.com`

## Reference Files

### Modified:
- `agentic-triage-system/src/main/java/com/pradeepl/triage/application/TriageWorkflow.java`

### Related:
- `agentic-triage-system/src/main/java/com/pradeepl/triage/application/agents/KnowledgeBaseAgent.java`
- `knowledge-base-mcp-server/src/main/java/com/pradeepl/knowledgebase/KnowledgeBaseEndpoint.java`
- `knowledge-base-mcp-server/src/main/resources/knowledge_base/*-runbook.md`
- `agentic-triage-system/src/main/java/com/pradeepl/triage/guardrails/PiiGuardrail.java`

## Previous Session Context

From yesterday's session, we completed:
- ✅ Created monorepo structure with parent POM
- ✅ Fixed compilation errors (RemoteMcpResources → RemoteMcpTools)
- ✅ Converted @McpResource to @McpTool (SDK 3.5.6 compatibility)
- ✅ Fixed service name in ClassifierAgent
- ✅ Verified all 3 services start successfully
- ✅ Confirmed PII guardrails are detecting emails in runbooks

## Git Status

**Local Changes**: TriageWorkflow.java modified
**Not Yet Pushed**: This fix needs to be committed and pushed to GitHub
**Repository**: https://github.com/PradeepLoganathan/akka-agentic-triage-demo
**Branch**: main
