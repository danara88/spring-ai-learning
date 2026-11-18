package com.devtalles.medassistant.tools;

import com.devtalles.medassistant.dto.DrugInfo;
import com.devtalles.medassistant.service.DrugInfoService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class DrugInfoTool {

	private final DrugInfoService drugInfoService;

	@Tool(description = "Buscar información oficial sobre un medicamento: " +
		"propósito, advertencias, dosis y contraindicaciones. " +
		"Usar cuando el usuario pregunte sobre un fármaco específico.")
	public DrugInfo getDrugInfo(
		@ToolParam(description = "Nombre genérico del medicamento en inglés, " +
			"por ejemplo: ibuprofen, aspirin, meetformin")
		String drugName
	) {
		log.info("[Tool] getDrugInfo invoked with name: {}", drugName);
		return drugInfoService.getDrugInfo(drugName);
	}
}
