package com.devtalles.medassistant.mcp;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

@Component
public class SystemConsultationPromptProvider {

	// Registramos el Prompt disponibla para el MCP (exponer)
	@McpPrompt(
		name = "symptom-consultation",
		description = "Genera un mnesaje estructurado para consultar sobre sintomas médicos"
	)
	public GetPromptResult symptomConsultation(
		@McpArg(
			name = "symptom",
			description = "Sintoma principal del paciente",
			required = true
		)
		String symptom
	) {
		String message = """
			Necesito orientación educativa sobre el siguiente sintoma: %s.
			
			Por favor incluye:
			- Posibles causas comunes
			- Cuándo consultar a un profesional
			- Especialidad médica recomendada
			
			Recuerda que esta información es solo educativa y no remplaza una consulta médica presencial.
			""".formatted(symptom);
		return GetPromptResult.builder(
				List.of(
					new PromptMessage(Role.USER, TextContent.builder(message).build()))) // Role.USER: Como si lo hubiera escrito el usuario
			.description("Consulta sobre sintomas")
			.build();
	}
}
