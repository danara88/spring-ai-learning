package com.devtalles.medassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("aws")
public class ClientResolverAws implements ClientResolver {

	private final ChatClient openAiClient;
	private final ChatModel openAiModel;
	private final VectorStore openAiVectorStore;

	public ClientResolverAws(
		@Qualifier("openAiClient") ChatClient openAiClient,
		@Qualifier("openAiChatModel") ChatModel openAiModel,
		@Qualifier("openAiVectorStore") VectorStore openAiVectorStore
	) {
		this.openAiClient = openAiClient;
		this.openAiModel = openAiModel;
		this.openAiVectorStore = openAiVectorStore;
	}

	@Override
	public ChatClient resolveClient(String model) {
		assertOpenAi(model);
		return openAiClient;
	}

	@Override
	public ChatModel resolveModel(String model) {
		assertOpenAi(model);
		return openAiModel;
	}

	@Override
	public VectorStore resolveVectorStore(String model) {
		assertOpenAi(model);
		return openAiVectorStore;
	}

	private void assertOpenAi(String model) {
		String normalized = model == null
			? "openai"
			: model.strip()
				.toLowerCase();
		if (! "openai".equals(normalized)) {
			throw new IllegalArgumentException(
				"Modelo no soportado en producción: %s. En AWS solo está disponible: openai".formatted(model));
		}
	}
}
