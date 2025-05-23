package br.com.ppaul.mochamind;

import br.com.ppaul.mochamind.services.LLMService;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.ModelType;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class IdentificadorDePerfil {
    private static final List<String> MODELOS = Arrays.asList(System.getenv("LLM_MODEL_NAME").split(","));

    public static void main(String[] args) {
        var promptSistema = """
                Seja direto e objetivo.

                Identifique o perfil de compra de cada cliente.

                A resposta deve ser no seguinte formato:

                Não faça nenhuma outra consideração ou explicação.

                Siga o exemplo para cada cliente:

                Cliente - (descreva o perfil do cliente com somente três palavras)

                """;

        var clientes = carregarClientesDoArquivo();
        var modelo = MODELOS.get(0);
        var quantidadeTokens = contarTokens(clientes);
        if (quantidadeTokens > 4096)
            modelo = MODELOS.get(1);
        System.out.println("Quantidade de Tokens: " + quantidadeTokens);
        System.out.println("Modelo: " + modelo);
        var request = ChatCompletionRequest
                .builder()
                .model(modelo)
                .messages(Arrays.asList(
                        new ChatMessage(
                                ChatMessageRole.SYSTEM.value(),
                                promptSistema),
                        new ChatMessage(
                                ChatMessageRole.SYSTEM.value(),
                                clientes)))
                .build();

        var service = new LLMService(Duration.ofMinutes(10));

        System.out.println(
                service
                        .createChatCompletion(request)
                        .getChoices().get(0).getMessage().getContent());
    }

    private static int contarTokens(String prompt) {
        var registry = Encodings.newDefaultEncodingRegistry();
        var enc = registry.getEncodingForModel(ModelType.GPT_4);
        return enc.countTokens(prompt);
    }

    private static String carregarClientesDoArquivo() {
        try {
            var path = Path.of(ClassLoader
                    .getSystemResource("lista_de_compras_100_clientes.csv")
                    .toURI());
            return Files.readAllLines(path).toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar o arquivo!", e);
        }
    }

}
