package com.devtalles.medassistant;

import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
@AllArgsConstructor
public class TestChatClient implements CommandLineRunner {

	private final ChatClient chatClient;

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Running TestChatClient");
		String response = chatClient.prompt("¿Qué es la fiebre?")
			.call()
			.content();
		System.out.println("RESPUESTA:");
		System.out.println(response);
	}
}
