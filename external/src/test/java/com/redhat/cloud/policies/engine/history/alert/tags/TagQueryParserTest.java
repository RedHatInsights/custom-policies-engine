package com.redhat.cloud.policies.engine.history.alert.tags;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.redhat.cloud.policies.engine.history.alert.tags.TagQueryOperator.EQUAL;
import static com.redhat.cloud.policies.engine.history.alert.tags.TagQueryOperator.LIKE;
import static com.redhat.cloud.policies.engine.history.alert.tags.TagQueryOperator.NOT_EQUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TagQueryParserTest {

    @Test
    public void testNullTagQuery() {
        assertTrue(TagQueryParser.parse(null).isEmpty());
    }

    @Test
    public void testBlankTagQuery() {
        assertTrue(TagQueryParser.parse("   ").isEmpty());
    }

    @Test
    public void testSingleEqualCondition() {
        String tagQuery = "alpha = 'bravo'";
        List<TagQueryCondition> conditions = TagQueryParser.parse(tagQuery);
        assertEquals(1, conditions.size());
        assertEquals("alpha", conditions.get(0).getKey());
        assertEquals(EQUAL, conditions.get(0).getOperator());
        assertEquals("bravo", conditions.get(0).getValue());
    }

    @Test
    public void testSingleNotEqualCondition() {
        String tagQuery = "charlie != 'delta'";
        List<TagQueryCondition> conditions = TagQueryParser.parse(tagQuery);
        assertEquals(1, conditions.size());
        assertEquals("charlie", conditions.get(0).getKey());
        assertEquals(NOT_EQUAL, conditions.get(0).getOperator());
        assertEquals("delta", conditions.get(0).getValue());
    }

    @Test
    public void testSingleMatchesCondition() {
        String tagQuery = "echo matches '*foxtrot*'";
        List<TagQueryCondition> conditions = TagQueryParser.parse(tagQuery);
        assertEquals(1, conditions.size());
        assertEquals("echo", conditions.get(0).getKey());
        assertEquals(LIKE, conditions.get(0).getOperator());
        assertEquals("foxtrot", conditions.get(0).getValue());
    }

    @Test
    public void testMultipleValidConditions() {
        String tagQuery = "golf != 'hotel' and india = 'juliet' AND kilo matCHes '*lima*'";
        List<TagQueryCondition> conditions = TagQueryParser.parse(tagQuery);
        assertEquals(3, conditions.size());

        assertEquals("golf", conditions.get(0).getKey());
        assertEquals(NOT_EQUAL, conditions.get(0).getOperator());
        assertEquals("hotel", conditions.get(0).getValue());

        assertEquals("india", conditions.get(1).getKey());
        assertEquals(EQUAL, conditions.get(1).getOperator());
        assertEquals("juliet", conditions.get(1).getValue());

        assertEquals("kilo", conditions.get(2).getKey());
        assertEquals(LIKE, conditions.get(2).getOperator());
        assertEquals("lima", conditions.get(2).getValue());
    }

    @Test
    public void testMultipleValidAndInvalidConditions() {
        String tagQuery = "mike MATCHES '*november*' AnD some unknown condition";
        List<TagQueryCondition> conditions = TagQueryParser.parse(tagQuery);
        assertEquals(1, conditions.size());

        assertEquals("mike", conditions.get(0).getKey());
        assertEquals(LIKE, conditions.get(0).getOperator());
        assertEquals("november", conditions.get(0).getValue());
    }

    @Test
    public void testNoValidCondition() {
        String tagQuery = "I do not match any valid tag query condition pattern";
        assertTrue(TagQueryParser.parse(tagQuery).isEmpty());
    }
}
