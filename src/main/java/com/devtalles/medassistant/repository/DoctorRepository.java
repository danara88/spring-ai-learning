package com.devtalles.medassistant.repository;

import com.devtalles.medassistant.domain.Doctor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

	List<Doctor> findBySpecialtyIgnoreCase(String specialty);

	/// Sabemos que el que ejecutará el modelo será el modelo. Por lo que, tiene que extraer del prompt "Martines" y
	/// pasarlo en las propiedades. De esta manera, podemos encontrar al doctor.
	List<Doctor> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
		String firstName, String lastName, String specialty);


}
