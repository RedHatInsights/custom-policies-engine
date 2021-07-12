package com.redhat.cloud.policies.engine.history.alert.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.JacksonCodec;
import org.hawkular.alerts.api.model.condition.ConditionEval;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.List;
import java.util.Set;

@Converter
public class ConditionEvalsConverter implements AttributeConverter<List<Set<ConditionEval>>, String> {

    @Override
    public String convertToDatabaseColumn(List<Set<ConditionEval>> attribute) {
        if (attribute == null) {
            return null;
        } else {
            return Json.encode(attribute);
        }
    }

    @Override
    public List<Set<ConditionEval>> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        } else {
            return JacksonCodec.decodeValue(dbData, new TypeReference<>() {});
        }
    }
}
