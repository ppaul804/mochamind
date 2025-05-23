package br.com.ppaul.mochamind;

import br.com.ppaul.mochamind.services.LLMService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AnaliseDeSentimentos {

    private static final List<String> MODELOS = Arrays.asList(System.getenv("LLM_MODEL_NAME").split(","));

    public static void main(String[] args) {
        try {

            var promptSistema = """
                    Você é um analisador de sentimentos de avaliações de produtos.
                    Escreva um parágrafo com até 50 palavras resumindo as avaliações e depois atribua qual o sentimento geral para o produto.
                    Identifique também 3 pontos fortes e 3 pontos fracos identificados a partir das avaliações.

                    #### Formato de saída
                    Nome do produto:
                    Resumo das avaliações: [resuma em até 50 palavras]
                    Sentimento geral: [deve ser: POSITIVO, NEUTRO ou NEGATIVO]
                    Pontos fortes: [3 bullets points]
                    Pontos fracos: [3 bullets points]
                    """;

            var diretorioAvaliacoes = Path.of("src/main/resources/avaliacoes");
            var arquivosDeAvaliacoes = Files.walk(diretorioAvaliacoes, 1)
                    .filter(path -> path.toString().endsWith(".txt"))
                    .collect(Collectors.toList());

            for (Path arquivo : arquivosDeAvaliacoes) {
                System.out.println("Iniciando a análise de sentimentos do produto: " + arquivo.getFileName());

                var promptUsuario = carregarArquivo(arquivo);

                var request = ChatCompletionRequest
                        .builder()
                        .model(MODELOS.get(0))
                        .messages(Arrays.asList(
                                new ChatMessage(
                                        ChatMessageRole.SYSTEM.value(),
                                        promptSistema),
                                new ChatMessage(
                                        ChatMessageRole.USER.value(),
                                        promptUsuario)))
                        .build();

                var service = new LLMService(Duration.ofSeconds(60));

                var resposta = service
                        .createChatCompletion(request)
                        .getChoices().get(0).getMessage().getContent();

                salvarAnalise(arquivo.getFileName().toString().replace(".txt", ""), resposta);
                System.out.println("Análise de sentimentos concluída para o produto: " + arquivo.getFileName());
            }

        } catch (Exception e) {
            System.out.println("Erro ao processar a análise de sentimentos: " + e.getMessage());
        }
    }

    private static String carregarArquivo(Path arquivo) {
        try {
            return Files.readAllLines(arquivo).toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar o arquivo!", e);
        }
    }

    private static void salvarAnalise(String arquivo, String analise) {
        try {
            var path = Path.of("src/main/resources/analises/analise-sentimentos-" + arquivo + ".md");
            Files.writeString(path, analise, StandardOpenOption.CREATE_NEW);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar o arquivo!", e);
        }
    }

}
