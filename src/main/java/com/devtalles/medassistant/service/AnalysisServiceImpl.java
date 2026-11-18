package com.devtalles.medassistant.service;

import com.devtalles.medassistant.config.ClientResolver;
import com.devtalles.medassistant.dto.analysis.ConditionSummary;
import com.devtalles.medassistant.dto.analysis.QueryClassification;
import com.devtalles.medassistant.dto.analysis.SymptomAnalysis;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AnalysisServiceImpl implements AnalysisService {

	private final ClientResolver clientResolver;

	@Value("classpath:prompts/structured_analysis_prompt.st")
	private Resource structuredAnalysisPrompt;

	private PromptTemplate structuredAnalysisTemplate;

	@PostConstruct
	void init() {
		structuredAnalysisTemplate = new PromptTemplate(structuredAnalysisPrompt);
	}

	public AnalysisServiceImpl(ClientResolver clientResolver) {
		this.clientResolver = clientResolver;
	}

	@Override
	public ConditionSummary summarizeCondition(String condition, String model, Long userId) {
		log.info("Summarizing condition: {} using model: {}", condition, model);
		// 1. Genera un JSON Scchema a partir de los campos record
		var converter = new BeanOutputConverter<>(ConditionSummary.class);

		// 2. Arma una instruccion de texto que le dice al modelo: tu respuesta tiene que ser un json con esta estructura.
		String format = converter.getFormat();
		log.info("Using format for ConditionSummary: {}", format);

		// 3. Esa instrucción se agrega al prompt.
		String prompt = """
			Proporciona un resumen médico educativo sobre: %s
			
			%s
			""".formatted(condition, format);

		// 4. Se llama al modelo
		String jsonResponse = clientResolver.resolveClient(model)
			.prompt(prompt)
			/// Agregamos a la request el userId para que el ChatClient sepa a qué conversación pertenece el mensaje.
			.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.call()
			.content();
		assert jsonResponse != null;
		log.info("Model response: {}", jsonResponse);

		// 5. Cuando el modelo responde con JSON, el converter lo parse a un ObjectMapper.
		return converter.convert(jsonResponse);
	}

	@Override
	public ConditionSummary summarizeConditionV2(String condition, String model, Long userId) {
		log.info("Summarizing condition V2: {} using model: {}", condition, model);
		return clientResolver.resolveClient(model)
			.prompt()
			.user("Proporiona un resumen médico educatiovo sobre: " + condition)
			/// Agregamos a la request el userId para que el ChatClient sepa a qué conversación pertenece el mensaje.
			.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.call()
			.entity(ConditionSummary.class);
	}

	@Override
	public List<ConditionSummary> listRelatedConditions(String symptoms, String model, Long userId) {
		log.info("Listing related conditions: {} using model: {}", symptoms, model);
		return clientResolver.resolveClient(model)
			.prompt()
			.user("Listar las tres condiciones médicas más probables " +
				"para estos síntomas: " + symptoms)
			/// Agregamos a la request el userId para que el ChatClient sepa a qué conversación pertenece el mensaje.
			.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.call()
			// Clase anónima que preservar la información genérica
			// Usar cuando llega un array de datos
			.entity(new ParameterizedTypeReference<>() {});
	}

	@Override
	public SymptomAnalysis analyzeSymptoms(String symptoms, String model, Long userId) {
		log.info("Analyzing symptoms: {} using model: {}", symptoms, model);
		String message = structuredAnalysisTemplate.render(Map.of("symptoms", symptoms));
		return clientResolver.resolveClient(model)
			.prompt(message)
			/// Agregamos a la request el userId para que el ChatClient sepa a qué conversación pertenece el mensaje.
			.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.call()
			.entity(SymptomAnalysis.class);
	}

	@Override
	public QueryClassification classifyQuery(String query, String model, Long userId) {
		log.info("Classifying query: {} using model: {}", query, model);
		// Antes teníamos un modelo muy cargado, pero ahora necesitamos un modelo limpio.
		return ChatClient.create(clientResolver.resolveModel(model))
			.prompt()
			/// Agregamos a la request el userId para que el ChatClient sepa a qué conversación pertenece el mensaje.
			.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.user("Clasifica la siguiente consulta de un paciente.  " +
				"Determina qué tipo de consulta es y eplica brevemente por qué. " +
				"Consulta del paciente: " + query)
			.call()
			.entity(QueryClassification.class);
	}
}
