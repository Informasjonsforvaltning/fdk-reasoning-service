package no.fdk.fdk_reasoning_service.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import java.io.File

const val LOCAL_SERVER_PORT = 5000

private val mockserver = WireMockServer(LOCAL_SERVER_PORT)

fun startMockServer() {
    if(!mockserver.isRunning) {
        mockserver.stubFor(get(urlEqualTo("/ping"))
            .willReturn(aResponse().withStatus(200)))

        mockserver.stubFor(get(urlEqualTo("/organizations"))
            .willReturn(ok(File("src/test/resources/orgs.ttl").readText())))
        mockserver.stubFor(get(urlEqualTo("/organizations/972417866"))
            .willReturn(ok(File("src/test/resources/org.ttl").readText())))
        mockserver.stubFor(get(urlMatching("/organizations/orgpath/.*"))
            .willReturn(ok("/GENERATED/ORGPATH")))
        mockserver.stubFor(get(urlEqualTo("/reference-data/los"))
            .willReturn(ok(File("src/test/resources/los.rdf").readText())))
        mockserver.stubFor(get(urlEqualTo("/reference-data/eu/eurovocs"))
            .willReturn(ok(File("src/test/resources/eurovocs.ttl").readText())))

        mockserver.start()
    }
}

fun stopMockServer() {

    if (mockserver.isRunning) mockserver.stop()

}
