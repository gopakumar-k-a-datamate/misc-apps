package org.datamate.pharmacy.adapter.in.rest;


import com.datamate.bedrock.framework.common.pagination.PaginatedResponse;
import com.datamate.bedrock.framework.common.pagination.PaginationHelper;
import org.datamate.authz.rest.dto.AllowedValueResponse;
import org.datamate.pharmacy.application.port.in.GetDoctorsUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pharmacy/doctors")
public class DoctorController {

    private final GetDoctorsUseCase getDoctorsUseCase;

    public DoctorController(GetDoctorsUseCase getDoctorsUseCase) {
        this.getDoctorsUseCase = getDoctorsUseCase;
    }

    @GetMapping
    public PaginatedResponse<AllowedValueResponse> getDoctors(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String search
    ) {
        int validPage = PaginationHelper.toZeroBasedPage(page);
        int validSize = PaginationHelper.validateLimit(size);

        return getDoctorsUseCase.execute(validPage, validSize, search);
    }
}
