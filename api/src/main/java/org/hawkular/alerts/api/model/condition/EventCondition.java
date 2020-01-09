package org.hawkular.alerts.api.model.condition;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.redhat.cloud.custompolicies.api.model.condition.expression.ExprParser;
import com.redhat.cloud.custompolicies.api.model.condition.expression.parser.ExpressionParser;
import org.hawkular.alerts.api.doc.DocModel;
import org.hawkular.alerts.api.doc.DocModelProperty;
import org.hawkular.alerts.api.model.event.Event;
import org.hawkular.alerts.api.model.trigger.Mode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.hawkular.alerts.api.util.Util.isEmpty;

/**
 * An <code>EventCondition</code> is used for condition evaluations over Event data using expressions.
 *
 * Expression is a comma separated list of the following 3 tokens structure:
 *
 * <event.field> <operator> <constant> [,<event.field> <operator> <constant>]*
 *
 * - <event.field> represent a fixed field of event structure or a key of tags.
 *   Supported fields are the following:
 *      - tenantId
 *      - id
 *      - ctime
 *      - text
 *      - category
 *      - tags.<key>
 *
 * - <operator> is a string representing a string/numeric operator, supported ones are:
 *   "==" equals
 *   "!=" not equals
 *   "starts" starts with String operator
 *   "ends" ends with String operator
 *   "contains" contains String operator
 *   "match" match String operator
 *   "<" less than
 *   "<=" less or equals than
 *   ">" greater than
 *   ">=" greater or equals than
 *   "==" equals
 *
 * - <constant> is a string that might be interpreted as a number if is not closed with single quotes or a string
 * constant if it is closed with single quotes
 * i.e. 23, 'test'
 *
 * A constant string can contain special character comma but escaped with backslash.
 * i.e. '\,test', 'test\,'
 *
 * So, putting everything together, a valid expression might look like:
 * event.id start 'IDXYZ', event.tag.category == 'Server', event.tag.from end '.com'
 *
 * A non valid expression will return false.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@DocModel(description = "An EventCondition is used for condition evaluations over Event data using expressions. + \n" +
        " + \n" +
        "Expression is a comma separated list of the following 3 tokens structure: + \n" +
        " + \n" +
        "<event.field> <operator> <constant> [,<event.field> <operator> <constant>]* + \n" +
        " + \n" +
        "<event.field> represent a fixed field of event structure or a key of tags. + \n" +
        "Supported fields are the following: + \n" +
        "- tenantId + \n" +
        "- id + \n" +
        "- ctime + \n" +
        "- text + \n" +
        "- category + \n" +
        "- tags.<key> + \n" +
        " + \n" +
        "<operator> is a string representing a string/numeric operator, supported ones are: + \n" +
        "\"==\" equals + \n" +
        "\"!=\" not equals + \n" +
        "\"starts\" starts with String operator + \n" +
        "\"ends\" ends with String operator + \n" +
        "\"contains\" contains String operator + \n" +
        "\"match\" match String operator + \n" +
        "\"<\" less than + \n" +
        "\"<=\" less or equals than + \n" +
        "\">\" greater than + \n" +
        "\">=\" greater or equals than + \n" +
        "\"==\" equals + \n" +
        " + \n" +
        "<constant> is a string that might be interpreted as a number if is not closed with single quotes or a " +
        "string constant if it is closed with single quotes + \n" +
        "i.e. 23, 'test' + \n" +
        " + \n" +
        "A constant string can contain special character comma but escaped with backslash. + \n" +
        "i.e '\\,test', 'test\\,' + \n" +
        " + \n" +
        "So, putting everything together, a valid expression might look like: + \n" +
        "event.id starts 'IDXYZ', event.tag.category == 'Server', event.tag.from end '.com' + \n" +
        " + \n" +
        "A non valid expression will return false. + \n")
public class EventCondition extends Condition {

    private static final long serialVersionUID = 1L;

    @JsonInclude
    private String dataId;

    @DocModelProperty(description = "Event expression used for this condition.",
            position = 0,
            required = false)
    @JsonInclude
    private String expression;

    public void setExpr(String expr) {
        this.expr = expr;
    }

    public String getExpr() {
        return expr;
    }

    @JsonInclude
    private String expr;

    public EventCondition() {
        this("", "", Mode.FIRING, 1, 1, null, null);
    }

    public EventCondition(String tenantId, String triggerId, String dataId, String expression) {
        this(tenantId, triggerId, Mode.FIRING, 1, 1, dataId, expression);
    }

    public EventCondition(String tenantId, String triggerId, Mode triggerMode, String dataId) {
        this(tenantId, triggerId, triggerMode, 1, 1, dataId, null);
    }

    /**
     * This constructor requires the tenantId be assigned prior to persistence. It can be used when
     * creating triggers via Rest, as the tenant will be assigned automatically.
     */
    public EventCondition(String triggerId, Mode triggerMode, String dataId) {
        this("", triggerId, triggerMode, 1, 1, dataId, null);
    }

    public EventCondition(String triggerId, Mode triggerMode, String dataId, String expression) {
        this("", triggerId, triggerMode, 1, 1, dataId, expression);
    }

    public EventCondition(String tenantId, String triggerId, Mode triggerMode, String dataId, String expression) {
        this(tenantId, triggerId, triggerMode, 1, 1, dataId, expression);
    }

    public EventCondition(String tenantId, String triggerId, int conditionSetSize, int conditionSetIndex,
            String dataId) {
        this(tenantId, triggerId, Mode.FIRING, conditionSetSize, conditionSetIndex, dataId, null);
    }

    public EventCondition(String tenantId, String triggerId, int conditionSetSize, int conditionSetIndex,
            String dataId, String expression) {
        this(tenantId, triggerId, Mode.FIRING, conditionSetSize, conditionSetIndex, dataId, expression);
    }

    public EventCondition(String tenantId, String triggerId, Mode triggerMode, int conditionSetSize,
            int conditionSetIndex, String dataId) {
        this(tenantId, triggerId, triggerMode, conditionSetSize, conditionSetIndex, dataId, null);
    }

    /**
     * This constructor requires the tenantId be assigned prior to persistence. It can be used when
     * creating triggers via Rest, as the tenant will be assigned automatically.
     */
    public EventCondition(String triggerId, Mode triggerMode, int conditionSetSize,
            int conditionSetIndex, String dataId, String expression) {
        this("", triggerId, triggerMode, conditionSetSize, conditionSetIndex, dataId, expression);
    }

    public EventCondition(String tenantId, String triggerId, Mode triggerMode, int conditionSetSize,
            int conditionSetIndex, String dataId, String expression) {
        super(tenantId, triggerId, triggerMode, conditionSetSize, conditionSetIndex, Type.EVENT);
        this.dataId = dataId;
        this.expression = expression;
        updateDisplayString();
        validate();
    }

    public EventCondition(EventCondition condition) {
        super(condition);

        this.dataId = condition.getDataId();
        this.expression = condition.getExpression();
        this.expr = condition.getExpr();
        validate();
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    @Override
    public String getDataId() {
        return dataId;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    private static Pattern cleanComma = Pattern.compile("\\\\,");

    public boolean match(Event value) {
        if (null == value) {
            return false;
        }
        if (isEmpty(expression) && isEmpty(expr)) {
            return true;
        }
        if(expr != null && !isEmpty(expr)) {
            // Process expr first
            return ExprParser.evaluate(value, expr);
        }
        List<String> expressions = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < expression.length(); i++) {
            if (expression.charAt(i) == ','
                    && (i == 0 || (i > 0 && expression.charAt(i - 1) != '\\'))) {
                expressions.add(cleanComma.matcher(expression.substring(j, i).trim()).replaceAll(","));
                j = i + 1;
            }
        }
        expressions.add(cleanComma.matcher(expression.substring(j).trim()).replaceAll(","));
        for (String expression : expressions) {
            if (!processExpression(expression, value)) {
                return false;
            }
        }
        return true;
    }

    private static final String TENANT_ID = "tenantId";
    private static final String ID = "id";
    private static final String CTIME = "ctime";
    private static final String TEXT = "text";
    private static final String CATEGORY = "category";
    private static final String TAGS = "tags.";
    private static final String FACTS = "facts.";

    private static final String EQ = "==";
    private static final String NON_EQ = "!=";
    private static final String STARTS = "starts";
    private static final String ENDS = "ends";
    private static final String CONTAINS = "contains";
    private static final String MATCHES = "matches";
    private static final String LT = "<";
    private static final String LTE = "<=";
    private static final String GT = ">";
    private static final String GTE = ">=";

    private boolean processExpression(String expression, Event value) {
        if (isEmpty(expression) || null == value) {
            return false;
        }
        String[] tokens = expression.split(" ");
        if (tokens.length < 3) {
            return false;
        }
        String eventField = tokens[0];
        String operator = tokens[1];
        String constant = tokens[2];
        for (int i = 3; i < tokens.length; ++i) {
            constant += " ";
            constant += tokens[i];
        }
        String sEventValue = null;
        Long lEventValue = null;
        String sConstantValue = null;
        Double dConstantValue = null;

        if (isEmpty(eventField)) {
            return false;
        }
        if (TENANT_ID.equals(eventField)) {
            sEventValue = value.getTenantId();
        } else if (ID.equals(eventField)) {
            sEventValue = value.getId();
        } else if (CTIME.equals(eventField)) {
            lEventValue = value.getCtime();
        } else if (TEXT.equals(eventField)) {
            sEventValue = value.getText();
        } else if (CATEGORY.equals(eventField)) {
            sEventValue = value.getCategory();
        } else if (eventField.startsWith(TAGS)) {
            // We get the key from tags.<key> string
            String key = eventField.substring(5);
            sEventValue = value.getTags().get(key);
        } else if (eventField.startsWith(FACTS)) {
            String key = eventField.substring(6);

            // facts.key.value.value.etc
            String[] subMap = key.split("\\.", 2);
            Object innerValue = value.getFacts().get(subMap[0]);

            while(subMap.length > 1) {
                if(innerValue instanceof Map) {
                    subMap = subMap[1].split("\\.", 2);
                    innerValue = ((Map) innerValue).get(subMap[0]);
                } else {
                    break;
                }
            }

            sEventValue = (String) innerValue;

            // TODO Needs support for arrays and arrays of maps also

            // TODO If arrayKey is integer, consider this as array
//            int arrayPos = key.indexOf("[");
//            if (arrayPos > -1) {
//                String outerKey = key.substring(0, arrayPos);
//                int endArrayPos = key.indexOf("]");
//                System.out.printf("Key: %s, %d, %d\n", key, arrayPos, endArrayPos);
//                String innerMapKey = key.substring(arrayPos+1, endArrayPos);
//                Object innerValue = value.getFacts().get(outerKey);
//                if (innerValue instanceof Map) {
//                    Map<String, String> innerValueMap = (Map) innerValue;
//                    sEventValue = innerValueMap.get(innerMapKey);
//                    System.out.printf("%s sEventValue: %s\n", innerMapKey, sEventValue);
//                }
//            } else {
//                sEventValue = (String) value.getFacts().get(key);
//            }
        }
        if (sEventValue == null && lEventValue == null) {
            return false;
        }
        if (constant == null) {
            return false;
        }
        int constantLength = constant.length();
        if (constant.charAt(0) == '\'' && constant.charAt(constantLength - 1) == '\'') {
            sConstantValue = constant.substring(1, constantLength - 1);
        } else if (constant.charAt(0) == '\'' && constant.charAt(constantLength - 1) != '\'') {
            return false;
        } else if (constant.charAt(0) != '\'' && constant.charAt(constantLength - 1) == '\'') {
            return false;
        } else {
            dConstantValue = Double.valueOf(constant);
        }

        if (EQ.equals(operator)) {
            if (sEventValue != null && sConstantValue != null) {
                return sEventValue.equals(sConstantValue);
            }
            if (lEventValue != null && dConstantValue != null) {
                return lEventValue.longValue() == dConstantValue.doubleValue();
            }
            return false;
        } else if (NON_EQ.equals(operator)) {
            if (sEventValue != null && sConstantValue != null) {
                return !sEventValue.equals(sConstantValue);
            }
            if (lEventValue != null && dConstantValue != null) {
                return lEventValue.longValue() != dConstantValue.doubleValue();
            }
            return false;
        } else if (STARTS.equals(operator)) {
            if (sEventValue != null && sConstantValue != null) {
                return sEventValue.startsWith(sConstantValue);
            }
            return false;
        } else if (ENDS.equals(operator)) {
            if (sEventValue != null && sConstantValue != null) {
                return sEventValue.endsWith(sConstantValue);
            }
            return false;
        } else if (CONTAINS.equals(operator)) {
            if (sEventValue != null && sConstantValue != null) {
                return sEventValue.contains(sConstantValue);
            }
            return false;
        } else if (MATCHES.equals(operator)) {
            if (sEventValue != null && sConstantValue != null) {
                return sEventValue.matches(sConstantValue);
            }
            return false;
        } else if (GT.equals(operator)) {
            Double dEventValue = lEventValue != null ? lEventValue.doubleValue() : null;
            dEventValue = sEventValue != null ? Double.valueOf(sEventValue) : dEventValue;
            if (dEventValue != null && dConstantValue != null) {
                return dEventValue > dConstantValue;
            }
            return false;
        } else if (GTE.equals(operator)) {
            Double dEventValue = lEventValue != null ? lEventValue.doubleValue() : null;
            dEventValue = sEventValue != null ? Double.valueOf(sEventValue) : dEventValue;
            if (dEventValue != null && dConstantValue != null) {
                return dEventValue >= dConstantValue;
            }
            return false;
        } else if (LT.equals(operator)) {
            Double dEventValue = lEventValue != null ? lEventValue.doubleValue() : null;
            dEventValue = sEventValue != null ? Double.valueOf(sEventValue) : dEventValue;
            if (dEventValue != null && dConstantValue != null) {
                return dEventValue < dConstantValue;
            }
            return false;
        } else if (LTE.equals(operator)) {
            Double dEventValue = lEventValue != null ? lEventValue.doubleValue() : null;
            dEventValue = sEventValue != null ? Double.valueOf(sEventValue) : dEventValue;
            if (dEventValue != null && dConstantValue != null) {
                return dEventValue <= dConstantValue;
            }
            return false;
        }
        return false;
    }

    @Override
    public void validate() {
        if(this.expr != null && !expr.isEmpty()) {
            ExprParser.validate(this.expr);
        }
    }

    @Override
    public void updateDisplayString() {
        String cond = this.expression == null || this.expression.isEmpty() ? this.expr : this.expression;
        String s = String.format("%s matches [%s]", this.dataId, cond);
        setDisplayString(s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        EventCondition that = (EventCondition) o;

        return !(expression != null ? !expression.equals(that.expression) : that.expression != null);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (expression != null ? expression.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EventCondition{" +
                "dataId='" + dataId + '\'' +
                ",expression='" + expression + '\'' +
                '}';
    }
}
