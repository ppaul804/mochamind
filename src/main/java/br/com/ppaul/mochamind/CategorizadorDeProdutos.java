package br.com.ppaul.mochamind;

import br.com.ppaul.mochamind.services.LLMService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import lombok.extern.java.Log;

import java.time.Duration;
import java.util.Arrays;
import java.util.Scanner;

@Log
public class CategorizadorDeProdutos {

    private static final String LLM_MODEL_NAME = System.getenv("LLM_MODEL_NAME");

    public static void main(String[] args) {
        log.info("Rodando o LLM com o modelo: " + LLM_MODEL_NAME);
        var leitor = new Scanner(System.in);
        System.out.print("Digite as categorias válidas: ");
        var categorias = leitor.nextLine();
        var system = """
                Apenas responda em Português.
                Você é um categorizador de produtos e deve responder apenas o nome da categoria do produto informado
                                
                Escolha uma categoria dentra a lista abaixo:
                            
                %s
                            
                ###### exemplo de uso:
                            
                Pergunta: Bola de futebol
                Resposta: Esportes
                
                ###### regras a serem seguidas:
                Caso o usuario pergunte algo que nao seja relacionado a categorizacao de produtos, voce deve responder que nao pode ajudar pois o seu papel é apenas responder a categoria dos produtos
                """.formatted(categorias);
        while (true) {
            System.out.print("Digite o nome do produto ou sair para encerrar a aplicação: ");
            var user = leitor.nextLine();
            if (user.equalsIgnoreCase("sair"))
                break;
            dispararRequisicao(user, system);
        }
    }

    private static void dispararRequisicao(String user, String system) {
        LLMService service = new LLMService("", Duration.ofMinutes(3));
        ChatCompletionRequest completionRequest = ChatCompletionRequest
                .builder()
                .model(LLM_MODEL_NAME)
                .messages(Arrays.asList(
                        new ChatMessage(ChatMessageRole.SYSTEM.value(), system),
                        new ChatMessage(ChatMessageRole.USER.value(), user)))
                .build();
        service.createChatCompletion(completionRequest)
                .getChoices()
                .forEach(c -> System.out.println(c.getMessage().getContent()));
    }

}