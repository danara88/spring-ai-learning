package com.devtalles.medassistant.config;

import com.devtalles.medassistant.tools.AppointmentBookingTool;
import com.devtalles.medassistant.tools.AppointmentSearchTool;
import com.devtalles.medassistant.tools.DoctorInfoTool;
import com.devtalles.medassistant.tools.DrugInfoTool;
import com.devtalles.medassistant.tools.PatientInfoTool;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

/// Este es el único lugar donde importamos interfaces de un proveedor específico. El resto de la aplicación es
/// agnósticos del proveedor (nuestra lógica de negocio)
@Configuration
@RequiredArgsConstructor
@Profile("!aws")
public class AssistantConfig {

	private final ChatMemory chatMemory;

	@Value("classpath:prompts/system_prompt.st")
	private Resource systemPromptResource;

	private final AppointmentSearchTool appointmentSearchTool;
	private final DoctorInfoTool doctorInfoTool;
	private final PatientInfoTool patientInfoTool;
	private final DrugInfoTool drugInfoTool;
	private final AppointmentBookingTool appointmentBookingTool;

	private final MedicalAuditAdvisor medicalAuditAdvisor;

	@Value("${spring.ai.vectorstore.chroma.url}")
	private String chromaUrl;

	/// Excluimos ChromaVectorStoreAutoConfiguration en application.yaml, lo que también elimina el bean ChromaApi que esa
	/// autoconfiguración crearía. Por eso lo definimos aquí a mano.
	@Bean
	public ChromaApi chromaApi(@Value("${spring.ai.vectorstore.chroma.url}") String chromaUrl) {
		return ChromaApi.builder()
			.baseUrl(chromaUrl)
			.build();
	}

	/// Si queremos cambiar dinamicamente entre modelos en runtime, tenemos que tenelor los dos profiels activos (ollama y
	/// openAi). Además, tenemos que tener los dos beans configurados. Este BEAN fue diseñado para el chat de
	/// medassistant, y los passos de un agente no son iguales a los del chat de medassistant, si no que son workers
	/// especializados que necesitan un clente limpio.
	@Bean("openAiClient")
	public ChatClient openAiClient(OpenAiChatModel chatModel, @Qualifier("openAiVectorStore") VectorStore vectorStore)
		throws IOException {
		/// El modelo no sabe cuál es la fecha. El modelo no tiene reloj. Es un motor de predicción de texto.
		String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
			.replace("{currentDate}",
				LocalDate.now()
					.toString());
		return ChatClient.builder(chatModel)
			/// Aplica un system prompt a todas las interacciones que pasen por este chat client.
			/// No importa si es OpenAI, Gemini, etc. Siempre reciben el mismo system prompt.
			/// El default system prompt siempre responde antes que nada.
			.defaultSystem(systemPrompt)
			.defaultTools(appointmentSearchTool, doctorInfoTool, patientInfoTool, drugInfoTool, appointmentBookingTool)
			.defaultAdvisors(
				/// La función de QuestionAnswerAdvisor es interceptar el prompt antes de mandarlo al LLM
				/// e inrequecerlo con información obtenida del VectorStore.
				QuestionAnswerAdvisor.builder(vectorStore)
					.searchRequest(SearchRequest.builder()
						/// Define la similitud mínima que debe tener un documento para ser considerado relevante.
						/// Ejemplo: Document A con 0.9, document B con 0.85 pero el documento C con 0.65 no entra.
						.similarityThreshold(0.7)
						/// De los documentos seleccionados del similarityThreshold, devuelve como máximo los 3 más similares
						.topK(3)
						.build())
//					.promptTemplate(new PromptTemplate("""
//						Contexto de documentos médicos (usalo si es relevante para la pregunta):
//						{question_answer_context}
//
//						Si la información no está en los documentos, puedes usar tus herramientas o tu conocimiento general
//						para responder.
//						"""))
					.build(), medicalAuditAdvisor)
			.build();
	}

	/// Si queremos cambiar dinamicamente entre modelos en runtime, tenemos que tenelor los dos profiels activos (ollama y
	/// openAi). Además, tenemos que tener los dos beans configurados.
	@Bean("ollamaClient")
	public ChatClient ollamaClient(OllamaChatModel chatModel, @Qualifier("ollamaVectorStore") VectorStore vectorStore)
		throws IOException {
		/// El modelo no sabe cuál es la fecha. El modelo no tiene reloj. Es un motor de predicción de texto.
		String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
			.replace("{currentDate}",
				LocalDate.now()
					.toString());
		return ChatClient.builder(chatModel)
			/// Aplica un system prompt a todas las interacciones que pasen por este chat client
			/// No importa si es OpenAI, Gemini, etc. Siempre reciben el mismo system prompt.
			.defaultSystem(systemPrompt)
			.defaultTools(appointmentSearchTool, doctorInfoTool, patientInfoTool, drugInfoTool, appointmentBookingTool)
			.defaultAdvisors(
				QuestionAnswerAdvisor.builder(vectorStore)
					.searchRequest(SearchRequest.builder()
						/// Define la similitud mínima que debe tener un documento para ser considerado relevante.
						/// Ejemplo: Document A con 0.9, document B con 0.85 pero el documento C con 0.65 no entra.
						.similarityThreshold(0.7)
						/// De los documentos seleccionados del similarityThreshold, devuelve como máximo los 3 más similares
						.topK(3)
						.build())
//					.promptTemplate(new PromptTemplate("""
//						Contexto de documentos médicos (usalo si es relevante para la pregunta):
//						{question_answer_context}
//
//						Si la información no está en los documentos, puedes usar tus herramientas o tu conocimiento general
//						para responder.
//						"""))
					.build(),
				medicalAuditAdvisor
			)
			.build();
	}

	/// Estos beans, no crean una base de datos. Lo que crea son dos clientes de tipo VectorStore configurados para
	/// conectarse con Chroma utilizando distintos modelos de embbedings. Además utilizan colecciones distintas.
	@Bean("openAiVectorStore")
	public VectorStore openAiVectorStore(
		@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel,
		ChromaApi chromaApi) {
		return ChromaVectorStore.builder(chromaApi, embeddingModel)
			.collectionName("medassistant_openai")
			.initializeSchema(true)
			.build();
	}

	/// Estos beans, no crean una base de datos. Lo que crea son dos clientes de tipo VectorStore configurados para
	/// conectarse con Chroma utilizando distintos modelos de embbedings. Además utilizan colecciones distintas.
	@Bean("ollamaVectorStore")
	public VectorStore ollamaVectorStore(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
		ChromaApi chromaApi) {
		return ChromaVectorStore.builder(chromaApi, embeddingModel)
			.collectionName("medassistant_ollama")
			.initializeSchema(true)
			.build();
	}
}
