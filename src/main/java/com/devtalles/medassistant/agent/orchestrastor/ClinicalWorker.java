package com.devtalles.medassistant.agent.orchestrastor;

import com.devtalles.medassistant.config.ClientResolver;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/// Este es el primer trabajador involucrado en nuestro orquestador
@Component
@Slf4j
@RequiredArgsConstructor
public class ClinicalWorker {

	private final ClientResolver clientResolver;

	@Value("classpath:prompts/worker-clinical.st")
	private Resource workerClinicalResource;

	private PromptTemplate workerClinicalPromptTemplate;

	@PostConstruct
	void init() {
		workerClinicalPromptTemplate = new PromptTemplate(workerClinicalResource);
	}

	public String analyse(String symptoms, String model) {
		String prompt = workerClinicalPromptTemplate.render(Map.of("sintomas", symptoms));
		String result = ChatClient.create(clientResolver.resolveModel(model))
			.prompt()
			.user(prompt)
			.system("Eres un especialista en razonamiento clínico.")
			.call()
			.content();
		log.info("Clinical analysis result: {}", result);
		return result;
	}
}
