package uk.co.stuffusell.api.client.client;

public class RequestContext {
    private static final ThreadLocal<RequestContext> THREAD_LOCAL = new ThreadLocal<>();
    private String authToken;

    public static RequestContext get() {
        RequestContext context = THREAD_LOCAL.get();
        if (context == null) {
            context = new RequestContext();
            THREAD_LOCAL.set(context);
        }
        return context;
    }

    public static RequestContext clear() {
        RequestContext context = THREAD_LOCAL.get();
        THREAD_LOCAL.remove();
        return context;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}
