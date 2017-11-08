package au.gov.ga.geodesy.support;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.exception.JsonQueryException;

/**
 * JSON Utilities
 */
public class Json {
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * Run jq over JSON.
     *
     * @param json JSON input
     * @param pattern jq expression
     * @return result of running jq over the input JSON
     *
     * @see <a href="https://stedolan.github.io/jq">https://stedolan.github.io/jq</a>
     * @see <a href="https://github.com/eiiches/jackson-jq">https://github.com/eiiches/jackson-jq</a>
     */
    public static List<JsonNode> jq(String json, String pattern) throws JsonQueryException, IOException {
        return JsonQuery.compile(pattern).apply(jsonMapper.readTree(json));
    }
}
