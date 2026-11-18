package com.devtalles.medassistant.config;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class MedicalAuditAdvisor implements CallAdvisor, StreamAdvisor {

	/// El adviseCall nos ayuda a interceptar cada llamada al LLM. Aqui podemos tener acceso a la entrada y salida de
	/// datos.
	@Override
	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
		String userMessage = chatClientRequest.prompt()
			.getUserMessage()
			.getText();
		String conversationId = (String) chatClientRequest.context()
			.getOrDefault(ChatMemory.CONVERSATION_ID, "uknown");

		log.info("MedicalAuditAdvisor - adviseCall - conversationId: {}, userMessage: {}", conversationId, userMessage);

		long start = System.currentTimeMillis();
		ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
		long elapsed = System.currentTimeMillis() - start;

		assert response.chatResponse() != null;
		String content = Objects.requireNonNull(response.chatResponse()
				.getResult())
			.getOutput()
			.getText();
		String preview = content != null && content.length() > 100
			? content.substring(0, 100) + "..."
			: content;

		log.info("AUDIT | conversationId: {} | tiempo: {} ms | respuesta: {}", conversationId, elapsed, preview);

		return response;
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
		StreamAdvisorChain streamAdvisorChain) {
		String userMessage = chatClientRequest.prompt()
			.getUserMessage()
			.getText();
		String conversationId = (String) chatClientRequest.context()
			.getOrDefault(ChatMemory.CONVERSATION_ID, "uknown");

		log.info("AUDIT | conversationId: {} | userMessage: {}", conversationId, userMessage);
		return streamAdvisorChain.nextStream(chatClientRequest);
	}

	@Override
	public String getName() {
		return "MedicalAuditAdvisor";
	}

	@Override
	public int getOrder() {
		// Si lo dejo en 0, nos va a mostrar un mensaje muy grande
		// De esta manera ordeno para que el advisor corra antes que otros advisors. Le estamos dando prioridad de ejecución.
		return Ordered.LOWEST_PRECEDENCE;
	}
}
