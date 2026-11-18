package com.devtalles.medassistant.config;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Slf4j
@EnableMethodSecurity // Vamos a crear un endpoint donde solo los admins pueden hacer una carga de recursos
public class SecurityConfig {

	/// Cada petición HTTP pasa por este filtro de seguridad antes de llegar al controller. Srping Security por defecto
	/// cree que vas a usar cookies de sesión. Por eso tenemos que desabilitar csrf. Las sesiones sirven para recordar al
	/// usuario. Cross-site request forgery (CSRF) es un ataque que se aprovecha de la confianza que un sitio tiene en el
	/// navegador del usuario. En nuestro caso estamos trabajando con STATELESS, el token tiene toda la información que
	/// necesitamos, el servidor no necesita recordar nada. Esta es la puerta de entrada al "edificio" tienes credenciales
	/// o no?
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		return http
			// Vamos a trabajar con stateless y no con statefull, por ello desabilitamos.
			// Tenerlos habilitado es un desperdicio de recursos.
			// Stateless: El token viaja en cada petición.
			// Statefull: Alamcenamos la sesión en el servidor.
			// ¿Por qué desabilitamos la protección csrf?
			// Se utiliza para proteger formularios web con cookies de sesión.
			// Sin sesión no hay cookie.
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session ->
				// Habilitamos STATELESS
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/mcp/**")
				.permitAll()
				.requestMatchers("/api/v1/**")
				.hasAnyRole("PATIENT", "ADMIN")
				.anyRequest()
				.authenticated()
			)
			// Aqui tenemos que pasar el converter que mapea los roles de Keycloak a roles de Java.
			.oauth2ResourceServer(auth2 ->
				auth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
			)
			.build();
	}

	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

		// Seteamos los roles
		// Mapea los roles que tenemos en Keycloak, a roles que tenemos en Java
		// Este supplier recibe el JasonWebToken
		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			// Obtenemos el objeto real_access (en keycloak, los roles viven dentro de este objeto)
			Map<String, Object> realAccess = jwt.getClaim("realm_access");
			if (realAccess == null) {
				log.error("No claims found for real_access attribute");
				return List.of();
			}

			Object rolesObj = realAccess.get("roles");
			if (! (rolesObj instanceof List<?> roles)) {
				log.error("Roles claim is not a list: {}", rolesObj);
				return List.of();
			}
			log.info("Found roles {}", roles);

			// Creamos una lista de roles que Spring Security entiende.
			// El prefijo ROLE_ es un detalle de implementación de Spring Security.
			// No conviene guardar directamente el el rol con ese prefijo director en keycloak,
			// ya que ese prefijo es solo para Sring Security. Si algún día queremos cambiar de proveedor y ese prefijo no se usa más,
			// tendríamos que renombrar todos los roles.
			return roles.stream()
				.filter(role -> role instanceof String)
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
				.collect(Collectors.toList());
		});

		return converter;
	}
}
