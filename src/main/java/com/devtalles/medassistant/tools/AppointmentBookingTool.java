package com.devtalles.medassistant.tools;

import com.devtalles.medassistant.service.AppointmentService;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AppointmentBookingTool {

	private final AppointmentService appointmentService;

	@Tool(description = "Reservar un turno médico para un paciente. " +
		"Solo personal administrativo puede ejecutar esta acción.")
	public String bookAppointment(
		@ToolParam(description = "Especialidad médica")
		String specialty,
		@ToolParam(description = "Fecha del turno en formato yyyy-MM-dd")
		String date,
		@ToolParam(description = "Hora del turno en formato HH:mm")
		String time,
		@ToolParam(description = "ID numérico del paciente")
		Long patientId,
		ToolContext context // No cualquiera puede hacer esta operación
	) {
		log.info("Booking appointment for patient {} with specialty {} on {} at {}", patientId, specialty, date, time);
		String role = context.getContext()
			.get("role")
			.toString();
		if (! "ADMIN".equals(role)) {
			log.warn("Denied access: role {}", role);
			return "No tienes permitido ejecutar esta acción.";
		}
		return appointmentService.bookAppointment(specialty, LocalDate.parse(date), LocalTime.parse(time), patientId);
	}
}
