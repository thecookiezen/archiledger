package com.thecookiezen.archiledger.agenticmemory.rag;

import com.embabel.agent.api.tool.callback.BeforeLlmCallContext;
import com.embabel.chat.Message;
import com.embabel.chat.SystemMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinalIterationWarningTransformerTest {

    private final List<Message> baseHistory = List.of(
            new SystemMessage("You are a helpful assistant.")
    );

    @Test
    void shouldNotModifyHistoryBeforeMaxIterations() {
        var transformer = new FinalIterationWarningTransformer(5);

        var context = new BeforeLlmCallContext(baseHistory, 3, List.of(), null);
        var result = transformer.transformBeforeLlmCall(context);

        assertEquals(baseHistory, result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldAppendWarningAtMaxIterations() {
        var transformer = new FinalIterationWarningTransformer(5);

        var context = new BeforeLlmCallContext(baseHistory, 5, List.of(), null);
        var result = transformer.transformBeforeLlmCall(context);

        assertEquals(2, result.size());
        assertEquals(baseHistory.get(0), result.get(0));
        assertInstanceOf(SystemMessage.class, result.get(1));
        assertTrue(((SystemMessage) result.get(1)).getContent().contains("final iteration"));
    }

    @Test
    void shouldUseCustomWarningMessage() {
        var transformer = new FinalIterationWarningTransformer(3, "Custom warning");

        var context = new BeforeLlmCallContext(baseHistory, 3, List.of(), null);
        var result = transformer.transformBeforeLlmCall(context);

        assertEquals(2, result.size());
        assertEquals("Custom warning", result.get(1).getContent());
    }

    @Test
    void shouldNotModifyHistoryAfterMaxIterationsIfIterationIsLower() {
        var transformer = new FinalIterationWarningTransformer(10);

        for (int i = 1; i < 10; i++) {
            var context = new BeforeLlmCallContext(baseHistory, i, List.of(), null);
            var result = transformer.transformBeforeLlmCall(context);
            assertEquals(baseHistory, result, "Should not modify at iteration " + i);
        }
    }
}
