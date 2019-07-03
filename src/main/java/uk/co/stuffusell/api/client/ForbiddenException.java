package uk.co.stuffusell.api.client;

import uk.co.stuffusell.api.common.ErrorResponse;

public class ForbiddenException extends SusServerException {
    public ForbiddenException(int statusCode, String statusMessage, ErrorResponse error) {
        super(statusCode, statusMessage, error);
    }
}
