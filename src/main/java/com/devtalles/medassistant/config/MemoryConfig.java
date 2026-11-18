package com.devtalles.medassistant.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryConfig {

	/// Esta es la abtracción. Por debajo puede usar la memoria RAM o una base de datos. Por conversación guardamos 10
	/// mensajes en el contexto. Por defecto usar usa memoria RAM con un HashMap concurrente en la JVM. Si reiniciamos la
	/// aplicación, perdermos la conversación. En producción no es aceptable usar memoria RAM ya que podemos perder
	/// contexto.
	@Bean
	ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
		/// MessageWindowChatMemory: Mantiene una conversación deslizante.
		/// Si tenemos 20 o 30 mensajes, solo va a tener en cuenta los últimos 10 mnesajes.
		/// Esto es importante porque controle el consume de tokens.
		/// Cada mensaje del histroial, cuenta como tokens de entrada.
		return MessageWindowChatMemory.builder()
			.chatMemoryRepository(chatMemoryRepository)
			/// Yo envio una peticion y el agente da su respuesta.
			/// En realidad son 5 mensajes mios y 5 mensajes de la AI. (5 intercambios)
			.maxMessages(10)
			.build();
	}
}
