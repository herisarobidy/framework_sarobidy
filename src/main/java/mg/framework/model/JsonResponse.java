package mg.framework.model;

public class JsonResponse {
    private String status;
    private int code;
    private Object data;
    private String message;

    public JsonResponse() {}

    public JsonResponse(String status, int code, Object data, String message) {
        this.status = status;
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}