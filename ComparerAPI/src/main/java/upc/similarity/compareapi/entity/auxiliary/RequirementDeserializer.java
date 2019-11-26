package upc.similarity.compareapi.entity.auxiliary;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import upc.similarity.compareapi.entity.Requirement;

import java.io.IOException;
import java.util.Iterator;

public class RequirementDeserializer extends StdDeserializer<Requirement> {

    public RequirementDeserializer() {
        this(null);
    }

    public RequirementDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Requirement deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        JsonNode idAux = node.get("id");
        JsonNode nameAux = node.get("name");
        JsonNode textAux = node.get("text");
        JsonNode statusAux = node.get("status");
        JsonNode createdAtAux = node.get("created_at");
        JsonNode modifiedAtAux = node.get("modified_at");
        JsonNode requirementParts = node.get("requirementParts");
        String component = deserializeComponent(requirementParts);
        String id = (idAux == null) ? null : idAux.asText();
        String name = (nameAux == null) ? null : nameAux.asText();
        String text = (textAux == null) ? null : textAux.asText();
        String status = (statusAux == null) ? null : statusAux.asText();
        long createdAt = (createdAtAux == null) ? 0 : createdAtAux.numberValue().longValue();
        long modifiedAt = (modifiedAtAux == null) ? 0 : modifiedAtAux.numberValue().longValue();
        return new Requirement(id,name,text,createdAt,modifiedAt,status,component);
    }

    private String deserializeComponent(JsonNode requirementParts) {
        if (requirementParts != null) {
            Iterator<JsonNode> iter = requirementParts.elements();
            while (iter.hasNext()) {
                JsonNode node = iter.next();
                JsonNode name = node.get("name");
                if (name != null && name.asText().equals("Components")) {
                    JsonNode textAux = node.get("text");
                    if (textAux != null) {
                        String text = textAux.asText();
                        text = text.replace("[\"", "");
                        text = text.replace("\"]", "");
                        text = text.replaceAll("\"", "");
                        return text;
                    }
                    return null;
                }
            }
        }
        return null;
    }
}
