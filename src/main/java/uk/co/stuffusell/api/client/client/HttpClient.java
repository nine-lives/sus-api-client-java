package uk.co.stuffusell.api.client.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import uk.co.stuffusell.api.client.Configuration;
import uk.co.stuffusell.api.client.ForbiddenException;
import uk.co.stuffusell.api.client.SusException;
import uk.co.stuffusell.api.client.SusServerException;
import uk.co.stuffusell.api.client.UnauthorisedException;
import uk.co.stuffusell.api.client.util.ObjectMapperFactory;
import uk.co.stuffusell.api.client.util.RateLimiter;
import uk.co.stuffusell.api.client.util.RequestParameterMapper;
import uk.co.stuffusell.api.common.ErrorResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;

public class HttpClient {
    private static final String HEADER_ACCESS_TOKEN = "X-Access-Token";
    private static final String HEADER_AUTH = HttpHeaders.AUTHORIZATION;
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final int TIMEOUT_MILLIS = -1;

    private final RequestParameterMapper parameterMapper = new RequestParameterMapper();
    private final ObjectMapper objectMapper = ObjectMapperFactory.make();
    private final Configuration configuration;
    private final CloseableHttpClient httpClient;
    private final ThreadLocal<HttpClientContext> httpContext = new ThreadLocal<>();
    private final RateLimiter rateLimiter;

    public HttpClient(Configuration configuration) {
        this.configuration = configuration;
        this.httpClient = makeHttpClient(configuration);
        this.rateLimiter = new RateLimiter(configuration.getRequestsPerSecond(), configuration.getRequestBurstSize());
    }

    public <T> T get(String path, Map<String, String> parameters, Class<T> responseType) {
        return executeAndTransform(new HttpGet(getUri(path, parameters)), responseType);
    }

    public <T> T get(String path, Map<String, String> parameters, TypeReference<T> responseType) {
        return executeAndTransform(new HttpGet(getUri(path, parameters)), responseType);
    }

    public <T> T post(String path, Object data, Class<T> responseType) {
        HttpPost request = setPayload(new HttpPost(getUri(path, null)), data);
        return executeAndTransform(request, responseType);
    }

    public <T> T delete(String path, Map<String, String> parameters, Class<T> responseType) {
        return executeAndTransform(new HttpDelete(getUri(path, parameters)), responseType);
    }

    private <T> T executeAndTransform(HttpUriRequest request, Class<T> responseType) {
        String content = null;
        try {
            content = execute(request);
            return content == null ? null : objectMapper.readValue(content, responseType);
        } catch (IOException e) {
            throw throwError(content, e);
        }
    }

    private <T> T executeAndTransform(HttpUriRequest request, TypeReference<T> responseType) {
        String content = null;
        try {
            content = execute(request);
            return content == null ? null : objectMapper.readValue(content, responseType);
        } catch (IOException e) {
            throw throwError(content, e);
        }
    }


    private String execute(HttpUriRequest request) throws IOException {
        if (configuration.isBlockTillRateLimitReset()) {
            rateLimiter.blockTillRateLimitReset();
        }

        String authToken = RequestContext.get().getAuthToken();
        if (authToken != null && !authToken.isEmpty()) {
            request.addHeader(HEADER_AUTH, "Basic " + authToken);
        }
        request.addHeader(HEADER_ACCESS_TOKEN, configuration.getAccessToken());
        request.addHeader(HEADER_USER_AGENT, configuration.getUserAgent());
        request.addHeader("Accepts", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request, getHttpContext())) {
            if (response.getStatusLine().getStatusCode() >= HttpStatus.SC_BAD_REQUEST) {
                throw throwError(response);
            }

            return readEntity(response);
        }
    }

    private <T extends HttpEntityEnclosingRequest> T setPayload(T request, Object payload) {
        try {
            StringEntity entity = new StringEntity(objectMapper.writeValueAsString(payload), Charset.forName("UTF-8"));
            entity.setContentType("application/json; charset=utf-8");
            request.setEntity(entity);
            return request;
        } catch (JsonProcessingException e) {
            throw new SusException(e);
        }
    }

    private SusException throwError(CloseableHttpResponse response) {
        Header contentType = response.getFirstHeader("Content-Type");
        if (contentType != null && contentType.getValue().startsWith("application/json")) {
            String content = null;
            try {
                content = readEntity(response);
                return throwError(
                        response.getStatusLine().getStatusCode(),
                        response.getStatusLine().getReasonPhrase(),
                        objectMapper.readValue(content, ErrorResponse.class));
            } catch (IOException ignore) {
                return throwError(
                        response.getStatusLine().getStatusCode(),
                        response.getStatusLine().getReasonPhrase(),
                        new ErrorResponse(content));
            }
        } else {
            return throwError(
                    response.getStatusLine().getStatusCode(),
                    response.getStatusLine().getReasonPhrase(),
                    null);
        }
    }

    private SusException throwError(int statusCode, String statusMessage, ErrorResponse error) {
        switch (statusCode) {
            case 401:
                return new UnauthorisedException(statusCode, statusMessage, error);
            case 403:
                return new ForbiddenException(statusCode, statusMessage, error);
            default:
                return new SusServerException(statusCode, statusMessage, error);
        }
    }

    private SusException throwError(String content, IOException e) {
        try {
            return new SusServerException(
                    HttpStatus.SC_OK,
                    "OK",
                    objectMapper.readValue(content, ErrorResponse.class));
        } catch (IOException ignore) {
            return new SusException(e);
        }
    }

    private String readEntity(CloseableHttpResponse response) throws IOException {
        if (response.getEntity() == null) {
            return null;
        }
        return EntityUtils.toString(response.getEntity());
    }


    private CloseableHttpClient makeHttpClient(Configuration configuration) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(configuration.getMaxConnectionsPerRoute());
        connectionManager.setDefaultMaxPerRoute(configuration.getMaxConnectionsPerRoute());
        return HttpClients.custom().setConnectionManager(connectionManager).build();
    }

    private HttpClientContext getHttpContext() {
        HttpClientContext context = httpContext.get();
        if (context == null) {
            context = HttpClientContext.create();
            context.setRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(TIMEOUT_MILLIS)
                    .setConnectTimeout(TIMEOUT_MILLIS)
                    .setConnectionRequestTimeout(TIMEOUT_MILLIS)
                    .build());
            httpContext.set(context);
        }
        return context;
    }

    private URI getUri(String path, Map<String, String> params) {
        StringBuilder uri = new StringBuilder(configuration.getEndpoint())
                .append("/")
                .append(path);

        if (params != null) {
            uri.append(parameterMapper.write(params));
        }

        try {
            return new URI(uri.toString());
        } catch (URISyntaxException e) {
            throw new SusException(e);
        }
    }
}
