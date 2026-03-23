package com.thecookiezen.archiledger.agenticmemory.config;

import com.thecookiezen.archiledger.agenticmemory.rag.AgenticMemoryMcpTools;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpToolsConfig {

    @Bean
    public List<ToolCallback> mcpTools(AgenticMemoryMcpTools agenticMemoryMcpTools) {
        return List.of(ToolCallbacks.from(agenticMemoryMcpTools));
    }
}
