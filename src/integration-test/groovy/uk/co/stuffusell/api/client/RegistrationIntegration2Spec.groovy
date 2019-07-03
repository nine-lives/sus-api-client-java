package uk.co.stuffusell.api.client

import spock.lang.Ignore
import uk.co.stuffusell.api.common.CustomerDto
import uk.co.stuffusell.api.common.PasswordResetRequest
import uk.co.stuffusell.api.common.PasswordResetRequestRequest
import uk.co.stuffusell.api.common.RegistrationRequest
import uk.co.stuffusell.api.common.RegistrationResponse
import uk.co.stuffusell.api.common.SuccessResponse

class RegistrationIntegration2Spec extends BaseIntegrationSpec {

    def "I can register a user"() {
        given:
        String email = "john.doe.${UUID.randomUUID().toString()}@test.com";
        RegistrationRequest request = new RegistrationRequest()
                .withName("John Doe")
                .withEmail(email)
                .withPhone("07879440892")
                .withPassword("secret123");

        when:
        RegistrationResponse response = client.register(request);

        then:
        response.customerUuid.length() == 36
        response.orderUuid.length() == 36
        response.orderId ==~ /\d{5}-\d{3}/

        when:
        boolean result = client.isUsenameAvailable(email).available

        then:
        !result

        when:
        result = client.isUsenameAvailable("rnd." + email).available

        then:
        result
    }

    def "I can request a password reset"() {
        given:
        RegistrationRequest request = newUser

        when:
        SuccessResponse response = client.passwordResetRequest(new PasswordResetRequestRequest(request.email))

        then:
        response.success
    }

    @Ignore
    def "I can reset my password"() {
        when:
        CustomerDto response = client.passwordReset(new PasswordResetRequest("0422a6c6-f781-4beb-aaff-80daf0b98d90", "secret123"));

        then:
        response.fullName == 'John Doe'
    }
}
