package com.devtalles.medassistant.agent;

import com.devtalles.medassistant.config.ClientResolver;
import com.devtalles.medassistant.dto.AppointmentInfo;
import com.devtalles.medassistant.dto.agent.AppointmentRequest;
import com.devtalles.medassistant.dto.agent.AppointmentSelection;
import com.devtalles.medassistant.service.AppointmentService;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentChainServiceimpl implements AppointmentChainService {

	private final ClientResolver clientResolver;
	private final AppointmentService appointmentService;
	private final PendingBookingStore pendingBookingStore;

	@Value("classpath:prompts/appointment-extraction.st")
	private Resource extractResource;

	@Value("classpath:prompts/appointment-confirmation.st")
	private Resource confirmationResource;

	private PromptTemplate extractPromptTemplate;
	private PromptTemplate confirmationPromptTemplate;

	@PostConstruct
	void setup() {
		extractPromptTemplate = new PromptTemplate(extractResource);
		confirmationPromptTemplate = new PromptTemplate(confirmationResource);
	}

	@Override
	public String bookAppointmentChain(String userRequest, String model, Long userId) {
		// 1. Interpreta el mensaje del usuario para extraer la especialidad y la fecha de la cita.
		AppointmentRequest extracted = interpret(userRequest, model);

		// 2. Usando el resultado del paso 1, buscar disponibilidad de horarios
		List<AppointmentInfo> available = search(extracted);

		// String confirmation = confirm(userRequest, available, model);

		// 3. Usando el resultado del paso 2, obtén la selección del usuario (specialty, date, time, doctorName, message)
		String selection = select(userRequest, available, model, userId);

		log.info("Chain completed with selection: {}", selection);

		return selection;
	}

	/// OJO: Entre que el chain encontró la disponibiliad de la cita y el paciente confirma, puede pasar tiempo. Otro
	/// paciente, podría haber reservado ese mismo turno. Por lo que antes de reservar debemos verificar si ese turno
	/// sigue todavía disponible.
	@Override
	public String confirmBooking(Long userId) {
		AppointmentSelection selection = pendingBookingStore.remove(userId);

		if (Objects.isNull(selection)) {
			return "No tienes ningún turno pendiente de confirmación.";
		}

		if (! isStillAvailable(selection)) {
			return "El turno que seleccionaste ya no está disponible.";
		}

		return book(selection, userId);
	}

	@Override
	public String cancelBooking(Long userId) {
		AppointmentSelection selection = pendingBookingStore.remove(userId);

		if (Objects.isNull(selection)) {
			return "No tienes ningún turno pendiente de confirmación.";
		}

		return "Turno cancelado. Puedes buscar otro cuando quieras.";
	}

	private String book(AppointmentSelection selection, Long userId) {
		String result = appointmentService.bookAppointment(selection.specialty(), LocalDate.parse(selection.date()),
			LocalTime.parse(selection.time()), userId);
		log.info("Booking completed with result: {}", result);
		return result;
	}

	private boolean isStillAvailable(AppointmentSelection selection) {
		List<AppointmentInfo> available = appointmentService.findAvailableAppointments(selection.specialty(),
			LocalDate.parse(selection.date()));
		return available.stream()
			.anyMatch(a -> a.time()
				.equals(selection.time()));
	}

	/// Paso 1: Interpretar la solicitud del usuario y extraer la especialidad y la fecha
	private AppointmentRequest interpret(String userRequest, String model) {
		String prompt = extractPromptTemplate.render(Map.of("pedido", userRequest));
		/// Aqui trabajamos con un model completamente limpio y no meter un cliente con mucha informacion.
		AppointmentRequest extracted = ChatClient.create(clientResolver.resolveModel(model))
			.prompt()
			.system("Eres un extractor de datos. Extrae exactamente lo que dice el texto, sin interpretar ni cambiar nada.")
			.user(prompt)
			.call()
			.entity(AppointmentRequest.class);
		log.info("Chain paso 1 - extraido: specialty: {}, date: {}", extracted.specialty(), extracted.date());
		return extracted;
	}

	/// Paso 2: Buscar citas disponibles en la especialidad y fecha extraídas
	private List<AppointmentInfo> search(AppointmentRequest extracted) {
		List<AppointmentInfo> available = appointmentService.findAvailableAppointments(extracted.specialty(),
			LocalDate.parse(extracted.date()));
		log.info("Chain paso 2 - Searching for appointments: available: {}, date: {}", available.size(), extracted.date());
		return available;
	}

	// Paso 3: Confirmar la cita con el usuario, mostrando las opciones disponibles
//	private String confirm(String userRequest, List<AppointmentInfo> available, String model) {
//		String prompt = confirmationPromptTemplate.render(Map.of("pedido", userRequest, "turnos", available.isEmpty()
//			? "Ninguno turno disponible."
//			// Pasar el array como String al template para que el modelo pueda procesarlo
//			: available.toString()));
//		log.info("Chain paso 3 - Confirming appointment: prompt: {}", prompt);
//		/// Aqui trabajamos con un model completamente limpio y no meter un cliente con mucha informacion.
//		return ChatClient.create(clientResolver.resolveModel(model))
//			.prompt()
//			.system("Eres un asistente de gestión de turnos médicos. Responde de forma breve y natural.")
//			.user(prompt)
//			.call()
//			.content();
//	}

	/// Paso 3
	private String select(String userRequest, List<AppointmentInfo> available, String model, Long userId) {
		if (available.isEmpty()) {
			return "No hay turnos disponibles para la especialidad y fecha solicitadas. Por favor, intenta con otra fecha o especialidad.";
		}
		AppointmentSelection selection = selectAppointment(userRequest, available, model);

		// Guardamos la cita seleccionada
		pendingBookingStore.store(userId, selection);

		return selection.message();
	}

	private AppointmentSelection selectAppointment(String userRequest, List<AppointmentInfo> available, String model) {
		String prompt = confirmationPromptTemplate.render(Map.of("pedido", userRequest, "turnos", available.toString()));
		return ChatClient.create(clientResolver.resolveModel(model))
			.prompt()
			.system("Eres un asistente de gestión de turnos médicos. Responde de forma breve y natural.")
			.user(prompt)
			.call()
			.entity(AppointmentSelection.class);
	}
}
