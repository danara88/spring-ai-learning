package com.devtalles.medassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!aws")
public class ClientResolverDefault implements ClientResolver {

	private final ChatClient openAiClient;
	private final ChatClient ollamaClient;

	private final ChatModel openAiModel;
	private final ChatModel ollamaModel;

	private final VectorStore openAiVectorStore;
	private final VectorStore ollamaVectorStore;

	public ClientResolverDefault(
		@Qualifier("openAiClient")
		ChatClient openAiClient,
		@Qualifier("ollamaClient")
		ChatClient ollamaClient,
		// Estos qualifiers ya existen.
		@Qualifier("openAiChatModel") ChatModel openAiModel,
		@Qualifier("ollamaChatModel") ChatModel ollamaModel,
		@Qualifier("openAiVectorStore") VectorStore openAiVectorStore,
		@Qualifier("ollamaVectorStore") VectorStore ollamaVectorStore) {
		this.openAiClient = openAiClient;
		this.ollamaClient = ollamaClient;

		this.openAiModel = openAiModel;
		this.ollamaModel = ollamaModel;

		this.openAiVectorStore = openAiVectorStore;
		this.ollamaVectorStore = ollamaVectorStore;
	}

	@Override
	public ChatClient resolveClient(String model) {
		return switch (normalize(model)) {
			case "openai" -> openAiClient;
			case "ollama" -> ollamaClient;
			default ->
				throw new IllegalArgumentException("Modelo no soportado: %s. Valores validos: openai, ollama".formatted(model));
		};
	}

	/// Necesario cuando necesitamos un client limpio sin nada.
	@Override
	public ChatModel resolveModel(String model) {
		return switch (normalize(model)) {
			case "openai" -> openAiModel;
			case "ollama" -> ollamaModel;
			default ->
				throw new IllegalArgumentException("Modelo no soportado: %s. Valores validos: openai, ollama".formatted(model));
		};
	}

	@Override
	public VectorStore resolveVectorStore(String model) {
		return switch (normalize(model)) {
			case "openai" -> openAiVectorStore;
			case "ollama" -> ollamaVectorStore;
			default ->
				throw new IllegalArgumentException("Modelo no soportado: %s. Valores validos: openai, ollama".formatted(model));
		};
	}

	private String normalize(String model) {
		return model == null
			? "openai"
			: model.strip()
				.toLowerCase();
	}
}
