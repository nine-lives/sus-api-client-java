package uk.co.stuffusell.api.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.codec.binary.Base64;
import uk.co.stuffusell.api.client.client.HttpClient;
import uk.co.stuffusell.api.client.client.RequestContext;
import uk.co.stuffusell.api.common.BookCourierRequest;
import uk.co.stuffusell.api.common.CustomerDto;
import uk.co.stuffusell.api.common.CustomerOrderDto;
import uk.co.stuffusell.api.common.CustomerOrderUpdateRequest;
import uk.co.stuffusell.api.common.CustomerUpdateRequest;
import uk.co.stuffusell.api.common.DateListDto;
import uk.co.stuffusell.api.common.InvoiceDto;
import uk.co.stuffusell.api.common.LedgerDto;
import uk.co.stuffusell.api.common.ListingReportDto;
import uk.co.stuffusell.api.common.LoginResponse;
import uk.co.stuffusell.api.common.PackagingRequestDto;
import uk.co.stuffusell.api.common.PackagingType;
import uk.co.stuffusell.api.common.PageDto;
import uk.co.stuffusell.api.common.PasswordResetRequest;
import uk.co.stuffusell.api.common.PasswordResetRequestRequest;
import uk.co.stuffusell.api.common.PricingChangedResponse;
import uk.co.stuffusell.api.common.PricingDto;
import uk.co.stuffusell.api.common.RegistrationRequest;
import uk.co.stuffusell.api.common.RegistrationResponse;
import uk.co.stuffusell.api.common.SalesTickerResponse;
import uk.co.stuffusell.api.common.StockReportSummaryDto;
import uk.co.stuffusell.api.common.SuccessResponse;
import uk.co.stuffusell.api.common.UserNameAvailableResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * SUS SDK entry point
 */
public final class SusClient {
    private final HttpClient client;

    private SusClient(Configuration configuration) {
        this.client = new HttpClient(configuration);
    }

    /**
     * Get a SUS instance for your given api key.
     *
     * @param accessToken your client id
     * @return a SUS instance
     */
    public static SusClient make(String accessToken) {
        return new SusClient(new Configuration()
                .withAccessToken(accessToken));
    }

    /**
     * Get a SUS instance for your given configuration.
     *
     * @param configuration your client id
     * @return a SUS instance
     */
    public static SusClient make(Configuration configuration) {
        return new SusClient(configuration);
    }

    public UserNameAvailableResponse isUsenameAvailable(String username) {
        return client.get(
                "/api/customer/username-available",
                ImmutableMap.of("username", username),
                UserNameAvailableResponse.class);
    }

    public RegistrationResponse register(RegistrationRequest registrationRequest) {
        return client.post(
                "/api/customer/register",
                registrationRequest,
                RegistrationResponse.class);
    }

    public SuccessResponse passwordResetRequest(PasswordResetRequestRequest request) {
        return client.post(
                "/api/customer/password-reset-request",
                request,
                SuccessResponse.class);
    }

    public CustomerDto passwordReset(PasswordResetRequest request) {
        return client.post(
                "/api/customer/password-reset",
                request,
                CustomerDto.class);
    }


    public SalesTickerResponse salesTicker() {
        return client.get(
                "/api/customer/sales-ticker",
                Collections.emptyMap(),
                SalesTickerResponse.class);
    }

    public List<String> categories() {
        return client.get(
                "/api/customer/categories",
                Collections.emptyMap(),
                new TypeReference<List<String>>() {
                });
    }

    public DateListDto getAvailableShippingDates() {
        return client.get(
                "/api/customer/shipping-dates",
                Collections.emptyMap(),
                DateListDto.class);
    }

    public LoginResponse login(String username, String password) {
        String authToken = authToken(username, password);
        RequestContext.get().setAuthToken(authToken);
        try {
            return new LoginResponse(authToken, client.get(
                    "/api/customer/login",
                    Collections.emptyMap(),
                    CustomerDto.class));
        } finally {
            RequestContext.clear();
        }
    }

    public CustomerDto current(String authToken) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.get(
                    "/api/customer/current",
                    Collections.emptyMap(),
                    CustomerDto.class);
        } finally {
            RequestContext.clear();
        }
    }

    public PricingDto pricing(String authToken) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.get(
                    "/api/customer/pricing",
                    Collections.emptyMap(),
                    PricingDto.class);
        } finally {
            RequestContext.clear();
        }
    }

    public LoginResponse update(String authToken, CustomerUpdateRequest request) {
        RequestContext.get().setAuthToken(authToken);
        try {
            CustomerDto customerDto = client.post(
                    "/api/customer/update",
                    request,
                    CustomerDto.class);
            String newAuthToken = authToken(
                    customerDto.getPrimaryEmail(),
                    request.getNewPassword() != null && !request.getNewPassword().isEmpty() ? request.getNewPassword() : request.getCurrentPassword());
            return new LoginResponse(newAuthToken, customerDto);
        } finally {
            RequestContext.clear();
        }
    }

    public PricingChangedResponse pricingChanged(String authToken) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.post(
                    "/api/customer/pricing-changed",
                    Collections.emptyMap(),
                    PricingChangedResponse.class);
        } finally {
            RequestContext.clear();
        }
    }

    public CustomerOrderDto newOrder(String authToken, boolean tcsAccepted, String ipAddress) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.post(
                    "/api/customer/new-order",
                    ImmutableMap.of("ipAddress", tcsAccepted ? ipAddress : ""),
                    CustomerOrderDto.class);
        } finally {
            RequestContext.clear();
        }
    }

    public List<CustomerOrderDto> orders(String authToken) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.get(
                    "/api/customer/orders",
                    Collections.emptyMap(),
                    new TypeReference<List<CustomerOrderDto>>() {
                    });
        } finally {
            RequestContext.clear();
        }
    }

    public CustomerOrderDto getOrder(String authToken, String sku) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.get(
                    "/api/customer/order/" + sku,
                    Collections.emptyMap(),
                    CustomerOrderDto.class);
        } finally {
            RequestContext.clear();
        }
    }

    public SuccessResponse updateOrder(String authToken, String sku, CustomerOrderUpdateRequest request) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.post(
                    "/api/customer/order/" + sku + "/update",
                    request,
                    SuccessResponse.class);
        } finally {
            RequestContext.clear();
        }
    }

    public List<String> getCollectPlusLabelNumbers(String authToken, String sku) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.get(
                    "/api/customer/order/" + sku + "/collect-plus-label-numbers",
                    Collections.emptyMap(),
                    new TypeReference<List<String>>() {
                    });
        } finally {
            RequestContext.clear();
        }
    }

    public SuccessResponse bookCourier(String authToken, String sku, BookCourierRequest request) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.post(
                    "/api/customer/order/" + sku + "/book-courier",
                    request,
                    SuccessResponse.class);
        } finally {
            RequestContext.clear();
        }
    }

    public List<PackagingRequestDto> getPackagingRequests(String authToken, String sku) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.get(
                    "/api/customer/order/" + sku + "/packaging-request",
                    Collections.emptyMap(),
                    new TypeReference<List<PackagingRequestDto>>() {
                    });
        } finally {
            RequestContext.clear();
        }
    }

    public SuccessResponse updatePackagingRequests(String authToken, String sku, Map<PackagingType, Integer> request) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.post(
                    "/api/customer/order/" + sku + "/packaging-request",
                    request,
                    SuccessResponse.class);
        } finally {
            RequestContext.clear();
        }
    }

    public SuccessResponse requestPayment(String authToken) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.post(
                    "/api/customer/customer-payment-request",
                    Collections.emptyMap(),
                    SuccessResponse.class);
        } finally {
            RequestContext.clear();
        }
    }

    public List<ListingReportDto> getListingHistory(String authToken, String sku) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.get(
                    "/api/customer/order/" + sku + "/listing-history",
                    Collections.emptyMap(),
                    new TypeReference<List<ListingReportDto>>() {
                    });
        } finally {
            RequestContext.clear();
        }
    }

    public StockReportSummaryDto getStockReportSummary(String authToken) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.get(
                    "/api/stock/summary",
                    Collections.emptyMap(),
                    StockReportSummaryDto.class);
        } finally {
            RequestContext.clear();
        }
    }

    public ListingReportDto getStockData(String authToken, String sku) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.get(
                    "/api/sku/" + sku,
                    Collections.emptyMap(),
                    ListingReportDto.class);
        } finally {
            RequestContext.clear();
        }
    }

    public LedgerDto getAccountLedger(String authToken) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.get(
                    "/api/customer/ledger",
                    Collections.emptyMap(),
                    LedgerDto.class);
        } finally {
            RequestContext.clear();
        }
    }

    public PageDto<InvoiceDto> getInvoices(String authToken, int page, int pageSize) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.get(
                    "/api/customer/invoices",
                    ImmutableMap.of("page", String.valueOf(page), "pageSize", String.valueOf(pageSize)),
                    new TypeReference<PageDto<InvoiceDto>>() {
                    });
        } finally {
            RequestContext.clear();
        }
    }

    public LedgerDto getInvoice(String authToken, String invoiceNumber) {
        RequestContext.get().setAuthToken(authToken);
        try {
            return client.get(
                    "/api/customer/invoice/" + invoiceNumber,
                    Collections.emptyMap(),
                    LedgerDto.class);
        } finally {
            RequestContext.clear();
        }
    }

    private String authToken(String username, String password) {
        String input = String.format("%s:%s", username, password);
        return Base64.encodeBase64String(input.getBytes());
    }
}
