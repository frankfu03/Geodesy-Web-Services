package au.gov.ga.geodesy.port.adapter.rest;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;

import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;

import au.gov.ga.geodesy.support.Json;
import au.gov.ga.geodesy.support.spring.IntegrationTest;

/**
 * Hit all endpoints.
 */
public class EndpointsITest extends IntegrationTest {

    /**
     * This test detects ambiguous endponts and potentially
     * some other problems as well.
     */
    @Test
    public void hitAllEndpoints() throws Exception {
        String rootResponse = given().get("/")
            .then()
            .statusCode(200)
            .log().body()
            .extract().body().asString();

        Json.jq(rootResponse, "._links[].href | capture(\"https://[^/]*(?<url>[^{]*)\") | .url").stream()
            .map(JsonNode::asText)
            .forEach(url -> given().get(url).then().statusCode(200));
    }
}
