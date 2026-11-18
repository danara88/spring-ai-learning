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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrchestratorServiceImpl implements OrchestratorService {

	private final ClientResolver clientResolver;

	private final ClinicalWorker clinicalWorker;
	private final BibliographyWorker bibliographyWorker;
	private final HistoryWorker historyWorker;

	@Value("classpath:prompts/orchestrator-synthesis.st")
	private Resource orchestratorSynthesisResource;

	private PromptTemplate orchestratorSynthesisPromptTemplate;

	@PostConstruct
	void init() {
		orchestratorSynthesisPromptTemplate = new PromptTemplate(orchestratorSynthesisResource);
	}

	@Override
	public String orchestrate(String symptoms, String model, Long userId) {
		log.info("Orquestador - iniciando análisis");

		String clinicalAnalysis = clinicalWorker.analyse(symptoms, model);
		String patientHistory = historyWorker.lookup(userId);
		String bibliographyResearch = bibliographyWorker.research(symptoms, model);

		log.info("Los tres workers terminaron su trabajo");

		return synthesiize(clinicalAnalysis, patientHistory, bibliographyResearch, symptoms, model);
	}

	private String synthesiize(String clinicalAnalysis, String patientHistory, String bibliographyResearch,
		String symptoms, String model) {
		String prompt = orchestratorSynthesisPromptTemplate.render(Map.of(
			"analisisClinico", clinicalAnalysis,
			"historial", patientHistory,
			"bibliografia", bibliographyResearch,
			"sintomas", symptoms
		));
		log.info("Orchestrator synthesis");
		return ChatClient.create(clientResolver.resolveModel(model))
			.prompt()
			.system(
				"Eres un médico que integra información de múltiples fuentes para dar una respuesta completa.")
			.user(prompt)
			.call()
			.content();
	}

	@Override
	public Flux<String> orchestrateWithProgress(String symptoms, String model, Long userId) {

		return Flux.<String>create(sink -> {
				try {
					sink.next("[PROGRESO] Analizando sintomas clínicamente...");
					String clinicalAnalysis = clinicalWorker.analyse(symptoms, model);

					sink.next("[PROGRESO] Consultando historial del paciente...");
					String patientHistory = historyWorker.lookup(userId);

					sink.next("[PROGRESO] Buscando bibliografia médica...");
					String bibliographyResearch = bibliographyWorker.research(symptoms, model);

					sink.next("[PROGRESO] Integrando todos los resultados...");
					String result = synthesiize(clinicalAnalysis, patientHistory, bibliographyResearch, symptoms, model);

					sink.next(result);
					sink.complete();
				} catch (Exception e) {
					log.error("Orquestados (progreso) - error: {}", e.getMessage());
					sink.error(e);
				}
			})
			.subscribeOn(
				Schedulers.boundedElastic()); // Significa que se va a ejecutar en un contexto de un recurso de un hilo. Liberamos el recurso HTTP y ejecuta las acciones en un contexto de pool de hilos.
	}
}
