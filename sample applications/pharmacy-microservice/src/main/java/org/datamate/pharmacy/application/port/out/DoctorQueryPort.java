package org.datamate.pharmacy.application.port.out;

import org.datamate.pharmacy.domain.model.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Outbound Port for querying doctor data.
 * Defines what the application layer needs from the outside world.
 * Implemented by the persistence adapter in the adapter layer.
 * This is a core concept of Hexagonal Architecture (Ports and Adapters).
 */
public interface DoctorQueryPort {
    Page<Doctor> findActiveDoctors(Pageable pageable);
    Page<Doctor> searchActiveDoctors(String search, Pageable pageable);
}
