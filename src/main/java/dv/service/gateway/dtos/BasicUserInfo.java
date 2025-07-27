package dv.service.gateway.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BasicUserInfo {
    private UUID userId;
    private String email;
    private String displayName;
    private String phoneNumber;
    private List<String> roles;
    private Boolean emailVerified;
}
