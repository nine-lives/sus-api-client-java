package uk.co.stuffusell.api.client

import spock.lang.Specification
import uk.co.stuffusell.api.client.util.ObjectMapperFactory
import uk.co.stuffusell.api.common.CustomerDto
import uk.co.stuffusell.api.common.LoginResponse
import uk.co.stuffusell.api.common.RegistrationRequest
import uk.co.stuffusell.api.common.RegistrationResponse

abstract class BaseIntegrationSpec extends Specification {

    protected static SusClient client

    def setupSpec() {
        ObjectMapperFactory.setFailOnUnknownProperties(true)
        client = SusClient.make(new Configuration()
                .withBlockTillRateLimitReset(false)
                .withEndpoint(System.getProperty("susEndpoint") ?: System.getenv("susEndpoint") ?: "http://localhost:8080")
                .withAccessToken(System.getProperty("susAccessToken") ?: System.getenv("susAccessToken")))
    }

    RegistrationRequest getNewUser() {
        String email = "john.doe.${UUID.randomUUID().toString()}@test.com";
        new RegistrationRequest()
                .withName("John Doe")
                .withEmail(email)
                .withPhone("07879440895")
                .withPassword("secret123")
    }

    String registerNewUser() {
        RegistrationRequest request = newUser
        client.register(request)
        request.email
    }

    LoginResponse registerAndLogin() {
        String email = registerNewUser()
        client.login(email, 'secret123')
    }

}
