package com.devtalles.medassistant.service;

import com.devtalles.medassistant.config.ClientResolver;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AssistantServiceImpl implements AssistantService {

	private final ClientResolver clientResolver;
	private final ChatMemory chatMemory;
	private MessageChatMemoryAdvisor memoryAdvisor;

	@Value("classpath:prompts/explain_condition_prompt.st")
	private Resource explainConditionPrompt;

	@Value("classpath:prompts/symptom_analysis_prompt.st")
	private Resource symptomAnalysisPrompt;

	@Value("classpath:prompts/diagnosis_cot_prompt.st")
	private Resource diagnosisCotPrompt;

	@Value("classpath:prompts/consultation_prompt.st")
	private Resource consultationPrompt;
	private PromptTemplate explainConditionTemplate;
	private PromptTemplate symptomAnalysisTemplate;
	private PromptTemplate diagnosisCotTemplate;
	private PromptTemplate consultationTemplate;

	public AssistantServiceImpl(ClientResolver clientResolver, ChatMemory chatMemory) {
		this.clientResolver = clientResolver;
		this.chatMemory = chatMemory;
	}

	/// Anteriormente, estamos creando un PromptTemplate en cada llamada. Ahora, solo creamos el PromptTemplate una vez.
	/// Cuando Spring crea nuestro servicio, Spring ejecuta el constructor. Dentro del constructor se inicializa los
	/// ChatClients, pero los de tipo Resouce no se crean/existen y es null. Por eso usamos PostConstruct. PostConstruct
	/// se ejecuta justamente después de ejecutar el constructor.
	@PostConstruct
	void init() {
		memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
			.build();
		explainConditionTemplate = new PromptTemplate(explainConditionPrompt);
		symptomAnalysisTemplate = new PromptTemplate(symptomAnalysisPrompt);
		diagnosisCotTemplate = new PromptTemplate(diagnosisCotPrompt);
		consultationTemplate = new PromptTemplate(consultationPrompt);
	}

	@Override
	public String chat(String prompt, String model, Long userId, String role) {
		log.info("Chat request - prompt: {}, model: {}", prompt, model);
		return clientResolver.resolveClient(model)
			.prompt(prompt)
			.toolContext(Map.of("userId", userId, "role", role))
			/// Necesita saber cuál es la conversación a la que pertenece el mensaje.
			/// Para eso, usamos un advisor que le dice al ChatClient cuál es el id de la conversación.
			/// Cada paciente tiene su propia conversación con el asistente.
			.advisors(a -> a.advisors(memoryAdvisor) // Gestionamos memoria por que esta pensado para un Chat.
				/// Agregamos a la request el userId para que el ChatClient sepa a qué conversación pertenece el mensaje.
				.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.call()
			.content();
	}

	@Override
	public Flux<String> chatStream(String prompt, String model, Long userId, String role) {
		log.info("Chat stream request - prompt: {}, model: {}, userId: {}, role: {}", prompt, model, userId, role);
		return clientResolver.resolveClient(model)
			.prompt(prompt)
			.toolContext(Map.of("userId", userId, "role", role))
			/// Necesita saber cuál es la conversación a la que pertenece el mensaje.
			/// Para eso, usamos un advisor que le dice al ChatClient cuál es el id de la conversación.
			/// Cada paciente tiene su propia conversación con el asistente.
			.advisors(a -> a.advisors(memoryAdvisor) // Gestionamos memoria por que esta pensado para un Chat.
				/// Agregamos a la request el userId para que el ChatClient sepa a qué conversación pertenece el mensaje.
				.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.stream()
			.content();
	}

	@Override
	public String explainCondition(String condition, String model) {
		log.info("Explain request - condition: {}, model: {}", condition, model);

		String message = explainConditionTemplate.render(Map.of("condition", condition));

		return clientResolver.resolveClient(model)
			.prompt(message)
			.call()
			.content();

//		return resolveClient(model)
//			.prompt()
//			.user(u -> u.text("Explicame qué es {condition} en terminos simples para un paciente."
//					+ "Usa lenguaje accesible y evita jerga médica innecesaria.")
//				.param("condition", condition))
//			.call()
//			.content();
	}

	@Override
	public String analyzeSymptoms(String symptoms, String model, Long userId) {
		log.info("Analyze request - symptoms: {}, model: {}, userId: {}", symptoms, model, userId);
		String message = symptomAnalysisTemplate.render(Map.of("sintomas", symptoms));
		return clientResolver.resolveClient(model)
			.prompt(message)
			/// Agregamos a la request el userId para que el ChatClient sepa a qué conversación pertenece el mensaje.
			.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.call()
			.content();
	}

	@Override
	public String diagnoseWithReasoning(String symptoms, String model, Long userId) {
		log.info("Diagnose request - symptoms: {}, model: {}, userId: {}", symptoms, model, userId);
		String message = diagnosisCotTemplate.render(Map.of("sintomas", symptoms));
		return clientResolver.resolveClient(model)
			.prompt(message)
			/// Agregamos a la request el userId para que el ChatClient sepa a qué conversación pertenece el mensaje.
			.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.call()
			.content();
	}

	@Override
	public String consult(String consult, String model, Long userId) {
		log.info("Consult request - consult: {}, model: {}, userId: {}", consult, model, userId);
		String message = consultationTemplate.render(Map.of("consulta", consult));
		return clientResolver.resolveClient(model)
			.prompt(message)
			/// Agregamos a la request el userId para que el ChatClient sepa a qué conversación pertenece el mensaje.
			.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.call()
			.content();
	}
}
