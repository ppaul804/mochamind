package br.com.ppaul.mochamind;

import br.com.ppaul.mochamind.services.LLMService;
import com.google.gson.Gson;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.apachecommons.CommonsLog;
import okhttp3.*;

import java.io.IOException;
import java.util.Arrays;

@CommonsLog
public class TestaIntegracao {

    public static void main(String[] args) {
        var sistem = "Você é um gerador de produtos fictícios para um ecommerce e deve gerar apenas o nome dos produtos solicitados pelo usuário";
        var user = "Gere 5 produtos";

        log.info("TESTANDO INTEGRAÇÃO COM O LLM STUDIO IMITANDO O OPENAI");
        testLlmStudio1(sistem, user, "deepseek-r1-distill-qwen-7b");
        log.info("TESTANDO INTEGRAÇÃO COM O OPENAI");
        testOpenAi(sistem, user, "gpt-4");
    }

    private static void testLlmStudio1(String sistem, String user, String model) {
        LLMService service = new LLMService("");
        ChatCompletionRequest completionRequest = ChatCompletionRequest
                .builder()
                .messages(Arrays.asList(
                        new ChatMessage(ChatMessageRole.SYSTEM.value(), sistem),
                        new ChatMessage(ChatMessageRole.USER.value(), user)))
                .model(model)
                .build();
        service.createChatCompletion(completionRequest)
                .getChoices()
                .forEach(c -> System.out.println(c.getMessage().getContent()));
    }

    private static void testOpenAi(String sistem, String user, String model) {
        var chave = System.getenv("OPENAI_API_KEY");
        if (chave == null) {
            log.error("Chave de API não encontrada. Defina a variável de ambiente OPENAI_API_KEY.");
            return;
        }
        OpenAiService service = new OpenAiService(chave);
        ChatCompletionRequest completionRequest = ChatCompletionRequest
                .builder()
                .messages(Arrays.asList(
                        new ChatMessage(ChatMessageRole.SYSTEM.value(), sistem),
                        new ChatMessage(ChatMessageRole.USER.value(), user)))
                .model(model)
                .build();
        service.createChatCompletion(completionRequest)
                .getChoices()
                .forEach(c -> System.out.println(c.getMessage().getContent()));
    }

    private static void testLlmStudio2(String sistem, String user, String model) {
        Gson gson = new Gson();
        var url = "http://127.0.0.1:1234/v1/chat/completions";
        ChatCompletionRequest completionRequestLocal = ChatCompletionRequest
                .builder()
                .model(model)
                .messages(Arrays.asList(
                        new ChatMessage(ChatMessageRole.SYSTEM.value(), sistem),
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
            result.getChoices().forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}