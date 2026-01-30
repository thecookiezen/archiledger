package com.example.memory.infrastructure.mcp;

import com.example.memory.application.service.KnowledgeGraphService;
import com.example.memory.domain.model.Entity;
import com.example.memory.domain.model.Relation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class McpToolAdapterTest {

    @Mock
    private KnowledgeGraphService service;

    @InjectMocks
    private McpToolAdapter adapter;

    @Test
    void createEntities() {
        adapter.createEntities(List.of(new Entity("test", "type")));
        verify(service).createEntities(anyList());
    }

    @Test
    void createRelations() {
        adapter.createRelations(List.of(new Relation("a", "b", "c")));
        verify(service).createRelations(anyList());
    }

    @Test
    void readGraph() {
        adapter.readGraph();
        verify(service).readGraph();
    }
}
