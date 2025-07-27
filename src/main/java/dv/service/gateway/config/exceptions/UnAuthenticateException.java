package dv.service.gateway.config.exceptions;

public class UnAuthenticateException extends RuntimeException {
    public static String MESSAGE = "You are not authenticated, please login again.";
}
