package hyun.messageconnecter.interceptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 인터셉터 체인 실행 중 공유되는 컨텍스트
 */
public class InterceptorContext {

    private final Object request;
    private final Method method;
    private final Class<?> responseType;
    private final ClientType clientType;

    private Object response;
    private Throwable error;
    private final Map<String, Object> attributes = new HashMap<>();

    public InterceptorContext(Object request, Method method, Class<?> responseType, ClientType clientType) {
        this.request = request;
        this.method = method;
        this.responseType = responseType;
        this.clientType = clientType;
    }

    // Getters
    public Object getRequest() {
        return request;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getResponseType() {
        return responseType;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public Object getResponse() {
        return response;
    }

    public Throwable getError() {
        return error;
    }

    // Setters
    public void setResponse(Object response) {
        this.response = response;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    // Attributes management
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }

    /**
     * 클라이언트 타입 (TCP)
     */
    public enum ClientType {
        TCP
    }
}
