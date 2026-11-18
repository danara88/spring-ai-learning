package com.devtalles.medassistant.service;

import com.devtalles.medassistant.dto.DrugInfo;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class DrugInfoServiceImpl implements DrugInfoService {

	private final RestClient openFdaClient;
	private static final int MAX_FIELD_LENGTH = 500; // Los warnings de medicamentos pueden ser muy largos. No queremos mandar mucha información al modelo.

	@Override
	public DrugInfo getDrugInfo(String drugName) {
		log.info("Consultando OpenFDA: drugName={}", drugName);
		Map<String, Object> response = fetchFromOpenFda(drugName);
		// Aquí iría la lógica para obtener la información del medicamento desde la API de OpenFDA
		var result = mapToDrugInfo(response, drugName);
		log.info("Consultando OpenFDA: result={}", result);
		return result;
	}

	private String extractFirst(Map<?, ?> map, String key) {
		if (! (map.get(key) instanceof List<?> list) || list.isEmpty()) {
			return "No disponible";
		}
		String value = list.getFirst()
			.toString();
		return value.length() > MAX_FIELD_LENGTH
			? value.substring(0, MAX_FIELD_LENGTH) + "..."
			: value;
	}

	private Map<String, Object> fetchFromOpenFda(String drugName) {
		return openFdaClient.get()
			.uri("/drug/label.json?search=openfda.generic_name:{name}&limit=1", drugName)
			.retrieve() // Es como si nosotros presionaramos el send en postman
			// Es cómo leemos la respuesta.
			// La respuesta llaga como json y queremos convertirla en objeto Java.
			// ParameterizedTypeReference: le dice a jackson, convierte este json a un map<String, Object>.
			.body(new ParameterizedTypeReference<>() {});
	}

	private DrugInfo mapToDrugInfo(Map<String, Object> response, String drugName) {
		// Este patron donde checamos la instacea tiene el nombre de: Pattern Matching
		if (! (response.get("results") instanceof List<?> results) || response.isEmpty()) {
			log.warn("No se encontraron resultados para el medicamento: {}", drugName);
			return null;
		}

		if (! (results.getFirst() instanceof Map<?, ?> result)) {
			log.warn("El resultado no es un mapa para el medicamento: {}", drugName);
			return null;
		}

		Map<?, ?> openFdaObj = result.get("openfda") instanceof Map<?, ?> m
			? m
			: Map.of();

		return new DrugInfo(
			extractFirst(openFdaObj, "brand_name"),
			extractFirst(openFdaObj, "generic_name"),
			extractFirst(result, "purpose"),
			extractFirst(result, "warnings"),
			extractFirst(result, "dosage_and_administration")
		);
	}
}
