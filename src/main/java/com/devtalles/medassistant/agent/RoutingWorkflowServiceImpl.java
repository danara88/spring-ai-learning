package com.devtalles.medassistant.agent;

import com.devtalles.medassistant.config.ClientResolver;
import com.devtalles.medassistant.config.MedicalAuditAdvisor;
import com.devtalles.medassistant.dto.analysis.QueryClassification;
import com.devtalles.medassistant.service.AnalysisService;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoutingWorkflowServiceImpl implements RoutingWorkflowService {

	private final ClientResolver clientResolver;
	private final AnalysisService analysisService;
	private final MedicalAuditAdvisor medicalAuditAdvisor;

	@Value("classpath:prompts/routing-emergency.st")
	private Resource routingEmergencyResource;

	@Value("classpath:prompts/routing-general.st")
	private Resource routingGeneralResource;

	@Value("classpath:prompts/routing-prescription.st")
	private Resource routingPrescriptionResource;

	@Value("classpath:prompts/routing-symptom.st")
	private Resource routingSymptomResource;

	private PromptTemplate routingEmergencyPromptTemplate;
	private PromptTemplate routingGeneralPromptTemplate;
	private PromptTemplate routingPrescriptionPromptTemplate;
	private PromptTemplate routingSymptomPromptTemplate;

	@PostConstruct
	void init() {
		routingEmergencyPromptTemplate = new PromptTemplate(routingEmergencyResource);
		routingGeneralPromptTemplate = new PromptTemplate(routingGeneralResource);
		routingPrescriptionPromptTemplate = new PromptTemplate(routingPrescriptionResource);
		routingSymptomPromptTemplate = new PromptTemplate(routingSymptomResource);
	}

	@Override
	public String routeQuery(String query, String model, Long userId) {
		QueryClassification classification = analysisService.classifyQuery(query, model, userId);
		return switch (classification.type()) {
			case EMERGENCY -> handleEmergency(query, model, userId);
			case GENERAL_QUESTION -> handleGeneralQuetion(query, model, userId);
			case SYMPTOM_REPORT -> handleSymptomAnalysis(query, model, userId);
			case PRESCRIPTION_RERQUEST -> handlePrescriptionRequest(query, model, userId);
			default -> handleOffTopic(query, model, userId);
		};
	}

	private String handleEmergency(String query, String model, Long userId) {
		log.info("Routa: EMERGENCY");
		String message = routingEmergencyPromptTemplate.render(Map.of("consulta", query));
		return ChatClient.create(clientResolver.resolveModel(model))
			.prompt()
			.system("Eres un especialista en urgencia médicas. Responde de forma breve y directa.")
			.user(message)
			.advisors(a -> a.advisors(medicalAuditAdvisor)
				.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.call()
			.content();
	}

	private String handleGeneralQuetion(String query, String model, Long userId) {
		log.info("Routa: GENERAL");
		String message = routingGeneralPromptTemplate.render(Map.of("consulta", query));
		return ChatClient.create(clientResolver.resolveModel(model))
			.prompt()
			.system("Eres un médico educador. Responde con detalle y claridad.")
			.user(message)
			.advisors(a -> a.advisors(medicalAuditAdvisor)
				.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.call()
			.content();
	}

	private String handleSymptomAnalysis(String query, String model, Long userId) {
		log.info("Routa: SYMPTOM");
		String message = routingSymptomPromptTemplate.render(Map.of("consulta", query));
		return ChatClient.create(clientResolver.resolveModel(model))
			.prompt()
			.system("Eres un médico que analiza síntomas. Responde con detalle y claridad.")
			.user(message)
			.advisors(a -> a.advisors(medicalAuditAdvisor)
				.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.call()
			.content();
	}

	private String handlePrescriptionRequest(String query, String model, Long userId) {
		log.info("Routa: PRESCRIPTION");
		String message = routingPrescriptionPromptTemplate.render(Map.of("consulta", query));
		return ChatClient.create(clientResolver.resolveModel(model))
			.prompt()
			.system("Eres un formacólogo clínico. Informas sobre medicamentos sin prescribir.")
			.user(message)
			.advisors(a -> a.advisors(medicalAuditAdvisor)
				.param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
			.call()
			.content();
	}

	private String handleOffTopic(String query, String model, Long userId) {
		log.info("Routa: OFF_TOPIC");
		return "Lo siento, solo puedo ayudarte con consultas relacionadas con salud y medicina. Por favor, formula una pregunta médica o relacionada con la salud.";
	}
}
