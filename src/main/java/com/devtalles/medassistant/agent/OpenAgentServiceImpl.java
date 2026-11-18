package com.devtalles.medassistant.agent;

import com.devtalles.medassistant.config.ClientResolver;
import com.devtalles.medassistant.tools.AppointmentSearchTool;
import com.devtalles.medassistant.tools.DoctorInfoTool;
import com.devtalles.medassistant.tools.DrugInfoTool;
import com.devtalles.medassistant.tools.PatientInfoTool;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;
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
public class OpenAgentServiceImpl implements OpenAgentService {

	private final ClientResolver clientResolver;
	private final PatientInfoTool patientInfoTool;
	private final AppointmentSearchTool appointmentSearchTool;
	private final DoctorInfoTool doctorInfoTool;
	private final DrugInfoTool drugInfoTool;

	@Value("classpath:prompts/open-agent.st")
	private Resource openAgentResource;

	private PromptTemplate openAgentPromptTemplate;

	@PostConstruct
	void setup() {
		openAgentPromptTemplate = new PromptTemplate(openAgentResource);
	}

	@Override
	public String execute(String query, String model, Long userId) {
		log.info("Executing Open Agent query: {}", query);

		String systemPrompt = openAgentPromptTemplate.render(Map.of("fechaActual", LocalDateTime.now()
			.toString()));

		String result = ChatClient.create(clientResolver.resolveModel(model))
			.prompt()
			.system(systemPrompt)
			.user(query)
			.tools(patientInfoTool, appointmentSearchTool, doctorInfoTool, drugInfoTool)
			.toolContext(Map.of("userId", userId, "role", "PATIENT"))
			.call()
			.content();
		
		log.info("Open Agent result: {}", result);

		return result;
	}
}
