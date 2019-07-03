package uk.co.stuffusell.api.client;

import uk.co.stuffusell.api.common.ErrorResponse;

public class UnauthorisedException extends SusServerException {
    public UnauthorisedException(int statusCode, String statusMessage, ErrorResponse error) {
        super(statusCode, statusMessage, error);
    }
}
