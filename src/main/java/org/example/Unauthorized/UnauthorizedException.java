package org.example.Unauthorized;

/**
 * استثنای سفارشی برای خطاهای احراز هویت
 */
public class UnauthorizedException extends RuntimeException {
    private final String errorCode;

    /**
     * سازنده با پیام خطا
     * @param message پیام خطا
     */
    public UnauthorizedException(String message) {
        super(message);
        this.errorCode = "UNAUTHORIZED";
    }

    /**
     * سازنده با پیام خطا و کد خطا
     * @param message پیام خطا
     * @param errorCode کد خطا (مثل INVALID_TOKEN, EXPIRED_TOKEN)
     */
    public UnauthorizedException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * دریافت کد خطا
     * @return کد خطا
     */
    public String getErrorCode() {
        return errorCode;
    }
}
