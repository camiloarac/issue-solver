package org.jetbrains.plugins.template;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.image.CreateImageRequest;
import io.reactivex.Flowable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MyOpenAIRequest {

    private String response;
    public MyOpenAIRequest(String requestText) {
        String token = "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";
        OpenAiService service = new OpenAiService(token, Duration.ofSeconds(30));

        System.out.println("Streaming chat completion...");
        final List<ChatMessage> messages = new ArrayList<>();
        final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), requestText);
        messages.add(systemMessage);
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                .builder()
                .model("gpt-3.5-turbo")
                .messages(messages)
                .n(1)
                .maxTokens(50)
                .logitBias(new HashMap<>())
                .build();

        Flowable<ChatCompletionChunk> flowableResult = service.streamChatCompletion(chatCompletionRequest);
        // Create a StringBuilder object to store the result
        StringBuilder buffer = new StringBuilder();
        // Subscribe to the Flowable object and print the result
        flowableResult.subscribe(chunk -> {
            chunk.getChoices().forEach(choice -> {
                String result = choice.getMessage().getContent();
                if (result != null) {
                    buffer.append(result);
                    System.out.print(choice.getMessage().getContent());
                }
            });
        }, Throwable::printStackTrace, () -> System.out.println());
        response = buffer.toString();
    }

    public String getResponse() {
        return response;
    }

}
