package uk.co.stuffusell.api.client

import uk.co.stuffusell.api.common.CustomerDto
import uk.co.stuffusell.api.common.CustomerOrderDto
import uk.co.stuffusell.api.common.CustomerUpdateRequest
import uk.co.stuffusell.api.common.LoginResponse
import uk.co.stuffusell.api.common.PaymentType
import uk.co.stuffusell.api.common.PricingChangedResponse

class CustomerIntegrationSpec extends BaseIntegrationSpec {

    def "I can log in and fetch the user for the token"() {
        given:
        String email = registerNewUser()

        when:
        LoginResponse response = client.login(email, 'secret123');

        then:
        response.customerDto.fullName == 'John Doe'
        response.customerDto.primaryEmail == email
        response.customerDto.primaryTelephone == '07879440895'

        when:
        CustomerDto me = client.current(response.authToken)

        then:
        me.fullName == 'John Doe'
        me.primaryEmail == email
        me.primaryTelephone == '07879440895'
    }

    def "I can update the user without changing credentials"() {
        given:
        LoginResponse login = registerAndLogin()
        CustomerUpdateRequest request = new CustomerUpdateRequest()
                .withEmail(login.getCustomerDto().getPrimaryEmail())
                .withPhone("07879440890")
                .withAddress1("Bedford Blues")
                .withAddress2("Goldington Road")
                .withCity("Bedford")
                .withPostcode("MK40 3NF")
                .withCountry("GBR")
                .withCurrentPassword("secret123")
                .withForename("Rory")
                .withSurname("Underwood")
                .withPayeeName("Tony Underwood")
                .withHasMarketPreference(false)
                .withDoNotDisturb(true)
                .withPaymentType(PaymentType.BACS)

        when:
        LoginResponse response = client.update(login.authToken, request);

        then:
        response.customerDto.payeeName == 'Tony Underwood'
        response.customerDto.fullName == 'Rory Underwood'
        response.customerDto.primaryEmail == login.getCustomerDto().getPrimaryEmail()
        response.customerDto.primaryTelephone == '07879440890'
        response.customerDto.address1 == 'Bedford Blues'
        response.customerDto.address2 == 'Goldington Road'
        response.customerDto.city == 'Bedford'
        response.customerDto.postcode == 'MK40 3NF'

        response.customerDto.country == 'United Kingdom'
        response.customerDto.countryIso3 == 'GBR'
        !response.customerDto.hasMarketPreference
        response.customerDto.doNotDisturb
        response.customerDto.paymentType == PaymentType.BACS

        when:
        CustomerDto me = client.current(response.authToken)

        then:
        response.customerDto.fullName == 'Rory Underwood'
    }

    def "I can update the user and change password"() {
        given:
        LoginResponse login = registerAndLogin()
        CustomerUpdateRequest request = new CustomerUpdateRequest(login.getCustomerDto())
                .withEmail(login.getCustomerDto().getPrimaryEmail())
                .withPhone("07879440890")
                .withCurrentPassword("secret123")
                .withNewPassword("secret321")

        when:
        LoginResponse response = client.update(login.authToken, request);

        then:
        response.customerDto.primaryTelephone == '07879440890'

        when:
        client.current(login.authToken)

        then:
        SusServerException serverException = thrown(SusServerException)
        serverException.statusCode == 401

        when:
        CustomerDto me = client.current(response.authToken)

        then:
        me.primaryTelephone == '07879440890'
    }

    def "I can update the user and change email and password"() {
        given:
        LoginResponse login = registerAndLogin()
        CustomerUpdateRequest request = new CustomerUpdateRequest(login.getCustomerDto())
                .withEmail("rnd." + login.getCustomerDto().getPrimaryEmail())
                .withPhone("07879440890")
                .withCurrentPassword("secret123")
                .withNewPassword("secret321")

        when:
        LoginResponse response = client.update(login.authToken, request);

        then:
        response.customerDto.primaryTelephone == '07879440890'

        when:
        client.current(login.authToken)

        then:
        SusServerException serverException = thrown(SusServerException)
        serverException.statusCode == 401

        when:
        CustomerDto me = client.current(response.authToken)

        then:
        me.primaryEmail == "rnd." + login.getCustomerDto().getPrimaryEmail()
        me.primaryTelephone == '07879440890'
    }

    def "I can check if the customer pricing has changed"() {
        given:
        LoginResponse login = registerAndLogin()

        when:
        PricingChangedResponse changed = client.pricingChanged(login.getAuthToken())

        then:
        !changed.hasPricingChanged
    }

    def "I can request a new order"() {
        given:
        LoginResponse login = registerAndLogin()

        when:
        CustomerOrderDto order = client.newOrder(login.getAuthToken())

        then:
        order.orderId ==~ /\d{5}-001/
    }

}
