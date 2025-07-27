package dv.service.gateway.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppResponse<T> {
    private int status;
    private String message;
    private T data;

    public AppResponse(HttpStatus status, String message, T data) {
        this.status = status.value();
        this.message = message;
        this.data = data;
    }

    public static <T> AppResponse<T> success() {
        return new AppResponse<>(HttpStatus.OK, null, null);
    }

    public static <T> AppResponse<T> success(T data, String message) {
        return new AppResponse<>(HttpStatus.OK, message, data);
    }

    public static <T> AppResponse<T> success(T data) {
        return new AppResponse<>(HttpStatus.OK, "Success", data);
    }

    public static <T> AppResponse<T> error(String message, HttpStatus status) {
        return new AppResponse<>(status, message, null);
    }

    public static <T> AppResponse<T> error(String message) {
        return new AppResponse<>(HttpStatus.BAD_REQUEST, message, null);
    }
}