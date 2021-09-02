package uk.co.stuffusell.api.client

import uk.co.stuffusell.api.common.*

class OrdersIntegrationSpec extends BaseIntegrationSpec {

    def "I can fetch orders and the order"() {
        given:
        LoginResponse login = registerAndLogin()

        when:
        List<CustomerOrderDto> orders = client.orders(login.authToken);

        then:
        orders.size() == 1
        orders[0].status == 'ORDER_RECEIVED'
        orders[0].orderId ==~ /\d{5}-001/

        when:
        CustomerOrderDto order = client.getOrder(login.authToken, orders[0].orderId);

        then:
        order.status == 'ORDER_RECEIVED'
        order.orderId ==~ /\d{5}-001/
        order.receivedDate == null
    }

    def "I can get an order"() {
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
        response.customerDto.primaryTelephone == '07879 440890'
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
        response.customerDto.primaryTelephone == '07879 440890'

        when:
        client.current(login.authToken)

        then:
        SusServerException serverException = thrown(SusServerException)
        serverException.statusCode == 401

        when:
        CustomerDto me = client.current(response.authToken)

        then:
        me.primaryTelephone == '07879 440890'
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
        response.customerDto.primaryTelephone == '07879 440890'

        when:
        client.current(login.authToken)

        then:
        SusServerException serverException = thrown(SusServerException)
        serverException.statusCode == 401

        when:
        CustomerDto me = client.current(response.authToken)

        then:
        me.primaryEmail == "rnd." + login.getCustomerDto().getPrimaryEmail()
        me.primaryTelephone == '07879 440890'
    }

    def "I can update the inventory list"() {
        given:
        LoginResponse login = registerAndLogin()
        CustomerOrderDto order = client.orders(login.authToken).get(0);
        CustomerOrderUpdateRequest request = new CustomerOrderUpdateRequest()
            .withItems([new CustomerItemDto().withCustomerDescription("My Item 1 RRP £123").withCustomerQuantity(2).withCustomerEstimate(40.0G),
                        new CustomerItemDto().withCustomerDescription("My Item 2").withCustomerQuantity(3),
                        new CustomerItemDto().withCustomerDescription("My Item 3")]);

        when:
        SuccessResponse response = client.updateOrder(login.getAuthToken(), order.orderId, request)

        then:
        response.success

        when:
        order = client.getOrder(login.authToken, order.orderId)

        then:
        order.items.size() == 3
        order.items[0].customerDescription == 'My Item 1 RRP £123'
        order.items[0].customerQuantity == 2
        order.items[0].customerEstimate == 40.0G
        order.items[1].customerDescription == 'My Item 2'
        order.items[1].customerQuantity == 3
        order.items[1].customerEstimate == null
        order.items[2].customerDescription == 'My Item 3'
        order.items[2].customerQuantity == 1
        order.items[2].customerEstimate == null
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
        CustomerOrderDto order = client.newOrder(login.getAuthToken(), true, "127.0.0.1")

        then:
        order.orderId ==~ /\d{5}-001/
    }

}
