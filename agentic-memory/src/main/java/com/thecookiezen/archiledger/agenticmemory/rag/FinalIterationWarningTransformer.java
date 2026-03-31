package com.thecookiezen.archiledger.agenticmemory.rag;

import com.embabel.agent.api.tool.callback.BeforeLlmCallContext;
import com.embabel.agent.api.tool.callback.ToolLoopTransformer;
import com.embabel.chat.Message;
import com.embabel.chat.SystemMessage;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ToolLoopTransformer} that monitors tool-loop iterations and injects a warning
 * {@link SystemMessage} when the iteration count reaches {@link #maxIterations}.
 * <p>
 * This prevents runaway tool calls by forcing the LLM to return its best answer on the
 * final allowed iteration. Configure the threshold via
 * {@link com.thecookiezen.archiledger.agenticmemory.AgenticMemoryProperties#maxToolIterations()}.
 */
public class FinalIterationWarningTransformer implements ToolLoopTransformer {

    private static final Logger logger = LoggerFactory.getLogger(FinalIterationWarningTransformer.class);

    private static final String DEFAULT_WARNING =
            "WARNING: This is your final iteration. You MUST NOT call any more tools. " +
            "Return your best answer based on the information gathered so far.";

    private final int maxIterations;
    private final String warningMessage;

    public FinalIterationWarningTransformer(int maxIterations) {
        this(maxIterations, DEFAULT_WARNING);
    }

    public FinalIterationWarningTransformer(int maxIterations, String warningMessage) {
        this.maxIterations = maxIterations;
        this.warningMessage = warningMessage;
    }

    @Override
    public List<Message> transformBeforeLlmCall(BeforeLlmCallContext context) {

        logger.info("testing testing =============== %d".formatted(context.getIteration()));

        if (context.getIteration() < maxIterations) {
            return context.getHistory();
        }

        List<Message> modified = new ArrayList<>(context.getHistory());
        modified.add(new SystemMessage(warningMessage));
        return modified;
    }
}
