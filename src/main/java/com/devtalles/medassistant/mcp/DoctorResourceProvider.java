package com.devtalles.medassistant.mcp;

import com.devtalles.medassistant.dto.DoctorInfo;
import com.devtalles.medassistant.service.DoctorService;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DoctorResourceProvider {

	private final DoctorService doctorService;

	@McpResource(
		// Cualquier MCP cliente conectado a nuestro server puede perdit doctor://cardiología y va a recibir el resultado
		uri = "doctor://{specialty}", // Esto es un recurso con template ya que tiene el {specialty}
		name = "Doctores por especialidad",
		description = "Lista de doctores disponibles para una especialidad médica"
	)
	public ReadResourceResult getDoctorBySpecialty(String specialty) {
		List<DoctorInfo> doctorInfos = doctorService.searchDoctors(specialty);
		String content = doctorInfos.isEmpty()
			? "No se encontraron doctores para la especialidad: " + specialty
			: listDoctors(doctorInfos);
		return ReadResourceResult.builder(
				List.of(
					new TextResourceContents("doctor://" + specialty, "text/plain", content)
				)
			)
			.build();
	}

	private String listDoctors(List<DoctorInfo> doctorInfos) {
		return doctorInfos.stream()
			.map(DoctorInfo::toString)
			.collect(Collectors.joining("\n"));
	}
}
