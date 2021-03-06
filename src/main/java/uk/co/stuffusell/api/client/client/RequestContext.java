package uk.co.stuffusell.api.client.client;

public class RequestContext {
    private static final ThreadLocal<RequestContext> THREAD_LOCAL = new ThreadLocal<>();
    private String authToken;
    private String ipAddress;
    private String userAgent;

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

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
