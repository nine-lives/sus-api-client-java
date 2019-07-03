package uk.co.stuffusell.api.client

import spock.lang.Ignore
import uk.co.stuffusell.api.common.*

class MetadataIntegrationSpec extends BaseIntegrationSpec {

    def "I can get the ticker data"() {

        when:
        SalesTickerResponse response = client.salesTicker();

        then:
        response.baseTime > 0
        response.currentEstimate > 0
        response.rateOfChange > 0
        response.baseTotal > 0
    }

    def "I can request categories"() {
        when:
        List<String>  response = client.categories()

        then:
        response.size() == 6
        response.contains('Electronics')
        response.contains('Fashion')
    }

    def "I can request shipping dates"() {
        when:
        DateListDto response = client.availableShippingDates

        then:
        response.dates.size() > 0
    }
}
