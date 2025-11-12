package com.pradeepl.triage.application.agents;

import akka.javasdk.agent.Agent;
import akka.javasdk.agent.ModelProvider;
import akka.javasdk.agent.MemoryProvider;
import akka.javasdk.agent.RemoteMcpTools;
import akka.javasdk.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(id = "knowledge-base-agent")
public class KnowledgeBaseAgent extends Agent {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseAgent.class);
    
    private static final String SYSTEM = """
        You are a knowledge base retrieval assistant with access to service runbooks via MCP tools.

        AVAILABLE MCP TOOLS:
        - You have access to knowledge base runbooks for various services
        - The knowledge-base-mcp-server provides runbooks and documentation
        - Available services include: payment-service, checkout-service, auth-service, api-gateway,
          order-service, user-service, and others

        TASK:
        1. Analyze the user's query to identify the relevant service(s)
        2. Use available MCP tools to retrieve runbook information
        3. Extract and summarize the relevant troubleshooting information
        4. Provide clear, actionable guidance based on the runbook content

        GUIDELINES:
        - If the query mentions a specific service, retrieve that service's runbook
        - If multiple services might be relevant, check multiple runbooks
        - If no specific service is mentioned, start with common services (payment, auth, api-gateway)
        - Always cite which runbook you're referencing
        - Be concise but thorough in your summaries

        Use the MCP tools available to access the knowledge base.
        """;

    public Effect<String> search(String query) {
        logger.info("🧠 KnowledgeBaseAgent.search() - invoking model with MCP tools for query: {}", query);
        logger.info("🔗 Using MCP tools from knowledge-base-mcp-server (port 9300)");

        return effects()
                .model(
                        ModelProvider.openAi()
                                .withApiKey(System.getenv("OPENAI_API_KEY"))
                                .withModelName("gpt-4o-mini")
                                .withTemperature(0.1)
                                .withMaxTokens(2000)
                )
                .memory(MemoryProvider.limitedWindow())
                .mcpTools(
                        RemoteMcpTools.fromService("knowledge-base-mcp-server")
                )
                .systemMessage(SYSTEM)
                .userMessage("Query: " + (query == null ? "" : query))
                .thenReply();
    }

}
