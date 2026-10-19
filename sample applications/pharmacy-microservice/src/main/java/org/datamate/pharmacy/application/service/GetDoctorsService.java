package org.datamate.pharmacy.application.service;

import com.datamate.bedrock.framework.common.pagination.PaginatedResponse;
import com.datamate.bedrock.framework.common.pagination.PaginationHelper;
import org.datamate.authz.rest.dto.AllowedValueResponse;
import org.datamate.pharmacy.application.port.in.GetDoctorsUseCase;
import org.datamate.pharmacy.application.port.out.DoctorQueryPort;
import org.datamate.pharmacy.domain.model.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GetDoctorsService implements GetDoctorsUseCase {

    private final DoctorQueryPort doctorQueryPort;

    public GetDoctorsService(DoctorQueryPort doctorQueryPort) {
        this.doctorQueryPort = doctorQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<AllowedValueResponse> execute(int page, int size, String search) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("name").ascending()
        );

        Page<Doctor> doctors;

        if (search == null || search.isBlank()) {
            doctors = doctorQueryPort.findActiveDoctors(pageable);
        } else {
            doctors = doctorQueryPort.searchActiveDoctors(search, pageable);
        }

        List<AllowedValueResponse> content = doctors
                .getContent()
                .stream()
                .map(doctor -> new AllowedValueResponse(
                        doctor.getId(),
                        doctor.getName()
                ))
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                new PaginatedResponse.PageMetadata(
                        PaginationHelper.toOneIndexed(doctors.getNumber()),
                        doctors.getSize(),
                        doctors.getTotalElements(),
                        doctors.getTotalPages()
                ),
                doctors.hasNext(),
                doctors.hasPrevious()
        );
    }
}
