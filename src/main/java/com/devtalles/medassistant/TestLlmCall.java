package com.devtalles.medassistant;

import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
@AllArgsConstructor
public class TestLlmCall implements CommandLineRunner {

	private final ChatModel chatModel;

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Running TestLlmCall");
		ChatResponse chatResponse = chatModel.call(new Prompt("¿Qué es la fiebre? Explicame en una oración."));

		// requireNonNull: Le decimos a Java que sabemos que no será null,
		// pero que si falla lanza un error detallado y no lances un not null pointer exception.
		String content = Objects.requireNonNull(chatResponse.getResult()).getOutput().getText();

		System.out.println("RESPUESTA:");
		System.out.println(content);

		System.out.println("Modelo: " + chatResponse.getMetadata().getModel());
		System.out.println("Tokens de entrada: " + chatResponse.getMetadata().getUsage().getPromptTokens());
		System.out.println("Tokens de salida: " + chatResponse.getMetadata().getUsage().getCompletionTokens());
		System.out.println("Tokens totales: " + chatResponse.getMetadata().getUsage().getTotalTokens());

		// Este es el motivo por el cuál el modelo dejó de generar tokens.
		// Algunas de las razones más comunes:
		// STOP: Terminó normalmente.
		// MAX TOKENS: Se cortó porque llegó al limite de tokens de salida.
		// SAFETY: La respuesta del modelo se bloquea porque detectó contenido sensible.
		System.out.println("Razón final: " + chatResponse.getResult().getMetadata().getFinishReason());
	}
}
