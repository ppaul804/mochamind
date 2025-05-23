package br.com.ppaul.mochamind;

import br.com.ppaul.mochamind.services.LLMService;
import com.google.gson.Gson;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.java.Log;
import okhttp3.*;

import java.io.IOException;
import java.util.Arrays;

@Log
public class TestaIntegracao {

    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String OPENAI_MODEL_NAME = System.getenv("OPENAI_MODEL_NAME");
    private static final String LLM_MODEL_NAME = System.getenv("LLM_MODEL_NAME");

    public static void main(String[] args) {
        var system = "Você é um gerador de produtos fictícios para um ecommerce e deve gerar apenas o nome dos produtos solicitados pelo usuário";
        var user = "Gere 5 produtos";

        log.info("Rodando o LLM com o modelo: " + LLM_MODEL_NAME);
        testLlmStudio1(system, user, LLM_MODEL_NAME);
        log.info("Rodando o OpenAI com o modelo: " + OPENAI_MODEL_NAME);
        testOpenAi(system, user, OPENAI_MODEL_NAME);
    }

    private static void testLlmStudio1(String system, String user, String model) {
        LLMService service = new LLMService("");
        ChatCompletionRequest completionRequest = ChatCompletionRequest
                .builder()
                .messages(Arrays.asList(
                        new ChatMessage(ChatMessageRole.SYSTEM.value(), system),
                        new ChatMessage(ChatMessageRole.USER.value(), user)))
                .model(model)
                .build();
        service.createChatCompletion(completionRequest)
                .getChoices()
                .forEach(c -> log.info(c.getMessage().getContent()));
    }

    private static void testOpenAi(String system, String user, String model) {
        if (OPENAI_API_KEY == null) {
            log.severe("Chave de API não encontrada. Defina a variável de ambiente OPENAI_API_KEY.");
            return;
        }
        OpenAiService service = new OpenAiService(OPENAI_API_KEY);
        ChatCompletionRequest completionRequest = ChatCompletionRequest
                .builder()
                .messages(Arrays.asList(
                        new ChatMessage(ChatMessageRole.SYSTEM.value(), system),
                        new ChatMessage(ChatMessageRole.USER.value(), user)))
                .model(model)
                .build();
        service.createChatCompletion(completionRequest)
                .getChoices()
                .forEach(c -> log.info(c.getMessage().getContent()));
    }

    private static void testLlmStudio2(String system, String user, String model) {
        Gson gson = new Gson();
        var url = "http://127.0.0.1:1234/v1/chat/completions";
        ChatCompletionRequest completionRequestLocal = ChatCompletionRequest
                .builder()
                .model(model)
                .messages(Arrays.asList(
                        new ChatMessage(ChatMessageRole.SYSTEM.value(), system),
                        new ChatMessage(ChatMessageRole.USER.value(), user)))
                .build();
        var client = new OkHttpClient();
        var request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.get("application/json"), gson.toJson(completionRequestLocal)))
                .build();
        try (var response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response);
            var body = response.body();
            if (body == null)
                throw new IOException("Response body is null");
            ChatCompletionResult result = gson.fromJson(body.string(), ChatCompletionResult.class);
            result.getChoices().forEach(t -> log.info(t.getMessage().getContent()));
        } catch (IOException e) {
            log.severe("Erro ao chamar o LLM Studio: " + e.getMessage());
        }
    }

}