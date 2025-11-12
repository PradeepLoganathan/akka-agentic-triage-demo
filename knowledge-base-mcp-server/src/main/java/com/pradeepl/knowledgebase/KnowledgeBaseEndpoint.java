package com.pradeepl.knowledgebase;

import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.mcp.McpEndpoint;
import akka.javasdk.annotations.mcp.McpResource;
import akka.javasdk.mcp.AbstractMcpEndpoint;

import com.pradeepl.knowledgebase.util.McpLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Knowledge Base MCP Server - Documentation & Runbooks
 *
 * This service provides access to knowledge base resources:
 * - RESOURCES: Service-specific troubleshooting runbooks
 * - RESOURCES: Team contact information
 * - RESOURCES: Historical incident reports
 *
 * Represents: Confluence, SharePoint, Jira, ServiceNow-like systems
 *
 * IMPORTANT FOR DEMO: Runbooks contain PII (team email addresses, phone numbers)
 * to demonstrate guardrail detection when accessed via MCP by KnowledgeBaseAgent.
 *
 * Consumed by: KnowledgeBaseAgent in agentic-triage-system
 */
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.ALL))
@McpEndpoint(serverName = "knowledge-base-mcp-server", serverVersion = "1.0.0")
public class KnowledgeBaseEndpoint extends AbstractMcpEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseEndpoint.class);

    // ==================== KNOWLEDGE BASE RESOURCES ====================
    // Resources for accessing service documentation and runbooks

    @McpResource(
        uriTemplate = "kb://runbooks/{serviceName}",
        name = "Service Runbook",
        description = "Get troubleshooting runbook for a specific service. Contains escalation contacts and procedures. May contain PII for guardrail demonstration.",
        mimeType = "text/markdown"
    )
    public String getRunbook(String serviceName) {
        // Log the incoming MCP resource access
        McpLogger.logResourceAccess("kb://runbooks/" + serviceName, "Service Runbook", serviceName);

        logger.info("📚 MCP Resource: getRunbook called - Service: {}", serviceName);

        try {
            String path = String.format("knowledge_base/%s-runbook.md", serviceName);
            InputStream in = getClass().getClassLoader().getResourceAsStream(path);

            if (in == null) {
                String notFoundResponse = String.format("# Runbook Not Found\n\nNo runbook available for service: %s", serviceName);
                McpLogger.logResourceResponse("kb://runbooks/" + serviceName, notFoundResponse.length(), false);
                return notFoundResponse;
            }

            String runbookContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            logger.info("📚 Runbook loaded - Service: {}, Size: {} bytes", serviceName, runbookContent.length());

            // Log the successful resource response
            McpLogger.logResourceResponse("kb://runbooks/" + serviceName, runbookContent.length(), true);

            return runbookContent;

        } catch (Exception e) {
            logger.error("📚 Error in getRunbook", e);
            McpLogger.logError("getRunbook", e);

            String errorResponse = String.format("# Error\n\nFailed to load runbook: %s", e.getMessage());
            McpLogger.logResourceResponse("kb://runbooks/" + serviceName, errorResponse.length(), false);
            return errorResponse;
        }
    }
}
