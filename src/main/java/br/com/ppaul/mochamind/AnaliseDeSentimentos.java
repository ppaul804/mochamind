package br.com.ppaul.mochamind;

import br.com.ppaul.mochamind.services.LLMService;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AnaliseDeSentimentos {

    private static final List<String> MODELOS = Arrays.asList(System.getenv("LLM_MODEL_NAME").split(","));

    public static void main(String[] args) {
        try {

            var arquivosDeAvaliacoes = carregarArquivosDeAvaliacoes();

            for (Path arquivo : arquivosDeAvaliacoes) {
                System.out.println("Iniciando a análise de sentimentos do produto: " + arquivo.getFileName());

                var resposta = enviarRequisicao(arquivo);

                salvarArquivoDeAnaliseDeSentimentos(arquivo, resposta);
                System.out.println("Análise de sentimentos concluída para o produto: " + arquivo.getFileName());
            }

        } catch (Exception e) {
            System.out.println("Erro ao processar a análise de sentimentos: " + e.getMessage());
        }
    }

    private static void salvarArquivoDeAnaliseDeSentimentos(Path arquivo, String resposta) {
        salvarAnalise(arquivo.getFileName().toString().replace(".txt", ""), resposta);
    }

    private static String enviarRequisicao(Path arquivo) throws InterruptedException {
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

        var segundosParaProximaTentativa = 5;
        var tentativas = 0;
        while (tentativas++ != 5) {
            try {
                return service
                        .createChatCompletion(request)
                        .getChoices().get(0).getMessage().getContent();
            } catch (OpenAiHttpException ex) {
                var errorCode = ex.statusCode;
                switch (errorCode) {
                    case 401:
                        throw new RuntimeException("Erro com a chave da API!", ex);
                    case 429: {
                        System.out.println("Rate Limit atingido! Nova tentativa em instantes...");
                        TimeUnit.SECONDS.sleep(segundosParaProximaTentativa);
                        segundosParaProximaTentativa *= 2;
                    }
                    case 500, 503: {
                        System.out.println("API fora do ar! Nova tentativa em instantes...");
                        TimeUnit.SECONDS.sleep(segundosParaProximaTentativa);
                        segundosParaProximaTentativa *= 2;
                    }
                    default:
                        break;
                }
            }
        }
        throw new RuntimeException("API fora do ar! Tentativa finalizadas");
    }

    private static List<Path> carregarArquivosDeAvaliacoes() throws IOException {
        var diretorioAvaliacoes = Path.of("src/main/resources/avaliacoes");
        var arquivosDeAvaliacoes = Files.walk(diretorioAvaliacoes, 1)
                .filter(path -> path.toString().endsWith(".txt"))
                .collect(Collectors.toList());
        return arquivosDeAvaliacoes;
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
