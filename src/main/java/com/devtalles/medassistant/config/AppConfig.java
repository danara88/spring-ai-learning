package com.devtalles.medassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

	/// Nuestro servicio es bloquante. WebClient es reactivo para servicios no bloqueantes. Todos los repositorios de JPA
	/// son bloqueantes. No tenemos un paradigmo reactivo. Si el proyectos es reactivo, utiliza WebClient. Si el proyecto
	/// es síncrono, RestClient.
	@Bean
	public RestClient openFdaClient() {
		return RestClient.builder()
			.baseUrl("https://api.fda.gov")
			.build();
	}
}
