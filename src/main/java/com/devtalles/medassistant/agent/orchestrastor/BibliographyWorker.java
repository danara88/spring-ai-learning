package com.devtalles.medassistant.agent.orchestrastor;

import com.devtalles.medassistant.config.ClientResolver;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/// Este es el tercer worker para nuestro orquestador, encargado de buscar bibliografía relevante para el caso clínico
@Component
@Slf4j
@RequiredArgsConstructor
public class BibliographyWorker {

	private final ClientResolver clientResolver;

	@Value("classpath:prompts/worker-bibliography.st")
	private Resource workerBibliographyResource;

	private PromptTemplate workerBibliographyTemplate;

	@PostConstruct
	void init() {
		workerBibliographyTemplate = new PromptTemplate(workerBibliographyResource);
	}

	private String interpret(List<Document> documents, String symptoms, String model) {
		/**
		 * La hipertención arterial...
		 *
		 * ---
		 *
		 * El protocolo de manejo de la hipertensión arterial...
		 */
		String documentContent = documents.stream()
			.map(Document::getText)
			.collect(Collectors.joining("\n\n---\n\n"));

		String prompt = workerBibliographyTemplate.render(Map.of(
			"documentos", documentContent,
			"sintomas", symptoms
		));

		String result = ChatClient.create(clientResolver.resolveModel(model))
			.prompt()
			.system("Eres un especialista en bibliografía médica.")
			.user(prompt)
			.call()
			.content();
		log.info("Bibliography analysis result: {}", result);
		return result;
	}

	private List<Document> searchDocuments(String symptoms, String model) {
		List<Document> documents = clientResolver.resolveVectorStore(model)
			.similaritySearch(
				SearchRequest.builder()
					.query(symptoms)
					.topK(3)
					// Todo lo que tenemos arriba del 70%, solo vamos a tomar 3 más altos.
					.similarityThreshold(0.7)
					.build()
			);
		log.info("Bibliography search result: {} documents found for symptoms: {}", documents.size(), symptoms);
		return documents;
	}

	public String research(String symtoms, String model) {
		List<Document> documents = searchDocuments(symtoms, model);
		if (documents.isEmpty()) {
			return "No se encontraron documentos relevantes para los síntomas proporcionados.";
		}
		return interpret(documents, symtoms, model);
	}
}
