package com.redhat.cloud.policies.engine.history.alert.tags;

import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.redhat.cloud.policies.engine.history.alert.tags.TagQueryOperator.LIKE;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class TagQueryParser {

    private static Logger LOGGER = Logger.getLogger(TagQueryParser.class);
    private static final String ANYTHING_BUT_SINGLE_QUOTES = "([^']+)";
    private static final Pattern PREFIX_REMOVAL_PATTERN = Pattern.compile("tags\\.", CASE_INSENSITIVE);
    private static final Pattern AND_PATTERN = Pattern.compile(" *and *", CASE_INSENSITIVE);
    private static final Pattern EQUAL_NOT_EQUAL_PATTERN = Pattern.compile(ANYTHING_BUT_SINGLE_QUOTES + " (!?=) '" + ANYTHING_BUT_SINGLE_QUOTES + "'");
    private static final Pattern MATCHES_PATTERN = Pattern.compile(ANYTHING_BUT_SINGLE_QUOTES + " matches '\\*" + ANYTHING_BUT_SINGLE_QUOTES + "\\*'", CASE_INSENSITIVE);

    /**
     * This method parses tag queries built by policies-ui-backend. It is only able to parse a very small subset of the
     * tag queries expression language from Hawkular. Never returns {@code null}.
     */
    public static List<TagQueryCondition> parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Collections.emptyList();
        }
        String noPrefixTagQuery = PREFIX_REMOVAL_PATTERN.matcher(rawQuery).replaceAll("");
        List<TagQueryCondition> conditions = new ArrayList<>();
        for (String rawCondition : AND_PATTERN.split(noPrefixTagQuery)) {
            TagQueryCondition condition = new TagQueryCondition();
            Matcher equalNotEqualMatcher = EQUAL_NOT_EQUAL_PATTERN.matcher(rawCondition);
            if (equalNotEqualMatcher.matches()) {
                TagQueryOperator operator = TagQueryOperator.fromString(equalNotEqualMatcher.group(2)).get();
                condition.setKey(equalNotEqualMatcher.group(1));
                condition.setOperator(operator);
                condition.setValue(equalNotEqualMatcher.group(3));
            } else {
                Matcher matchesMatcher = MATCHES_PATTERN.matcher(rawCondition);
                if (matchesMatcher.matches()) {
                    condition.setKey(matchesMatcher.group(1));
                    condition.setOperator(LIKE);
                    condition.setValue(matchesMatcher.group(2));
                } else {
                    LOGGER.warnf("Tag condition ignored because of unknown pattern: %s", rawCondition);
                    continue;
                }
            }
            conditions.add(condition);
        }
        return conditions;
    }
}
