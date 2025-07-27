package dv.service.gateway.controllers.healcheck;

import dv.service.gateway.dtos.AppResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealCheckController {

    @GetMapping("/health-check")
    public AppResponse<String> healthCheck() {
        return AppResponse.success(null, "Health Check");
    }
}
