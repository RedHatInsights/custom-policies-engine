package org.hawkular.alerts.api.model.condition;

import java.util.Locale;
import java.util.Objects;

import org.hawkular.alerts.api.doc.DocModel;
import org.hawkular.alerts.api.doc.DocModelProperty;
import org.hawkular.alerts.api.model.trigger.Mode;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A threshold condition against rate of change over time. Typically used for "counter" metrics, that continuously
 * increase or decrease.  Given the last two datums for dataId:
 * <pre>
 *   deltaTime   = datum.time - prevDatum.time
 *   deltaValue  = datum.value - prevData.value
 *   periods     = deltaTime / <conditionPeriod>
 *   rate        = deltaValue / periods
 *   match       = rate <conditionOperator> <conditionThreshold>
 * </pre>
 *
 * In other words, take the rate of change for the most recent datums and compare it to the threshold. For example,
 * Let's say we have a metric, sessionCount, that increments for each new session.  If the sessionCount increases
 * too quickly, say more than 20 per minute, we want an alert.  We'd want:
 * <pre>
 *   RateCondition( 'SessionCount', INCREASING, MINUTE, GT, 20 )
 * </pre>
 *
 * By specifying the SessionCount data as increasing, we know to ignore/reset if the previous session count is
 * less than the current session count.  This indicates that maybe the counter was reset (maybe due to a restart).
 * <p>
 * Note that rate of change is always determined as an absolute value. So threshold values should be >= 0.
 * </p>
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@DocModel(description = "A threshold condition against rate of change over time. + \n" +
        " + \n" +
        "Typically used for \"counter\" metrics, that continuously increase or decrease. + \n" +
        "Given the last two datums for dataId: + \n" +
        " + \n" +
        "deltaTime   = datum.time - prevDatum.time + \n" +
        "deltaValue  = datum.value - prevData.value + \n" +
        "periods     = deltaTime / <conditionPeriod> + \n" +
        "rate        = deltaValue / periods + \n" +
        "match       = rate <conditionOperator> <conditionThreshold> + \n" +
        " + \n" +
        "In other words, take the rate of change for the most recent datums and compare it to the threshold. + \n" +
        "For example, + \n" +
        "Let's say we have a metric, sessionCount, that increments for each new session.  If the sessionCount " +
        "increases too quickly, say more than 20 per minute, we want an alert.  We'd want: + \n" +
        " + \n" +
        "RateCondition( 'SessionCount', INCREASING, MINUTE, GT, 20 ) + \n" +
        " + \n" +
        "By specifying the SessionCount data as increasing, we know to ignore/reset if the previous session count is " +
        "less than the current session count.  This indicates that maybe the counter was reset " +
        "(maybe due to a restart). + \n" +
        " + \n" +
        "Note that rate of change is always determined as an absolute value. So threshold values should be >= 0.")
public class RateCondition extends Condition {

    private static final long serialVersionUID = 1L;

    public enum Operator {
        LT, GT, LTE, GTE
    }

    /** Default: MINUTE */
    public enum Period {
        SECOND(1000L), MINUTE(60000L), HOUR(60000L * 60), DAY(60000L * 60 * 24), WEEK(60000L * 60 * 24 * 7);

        private final long milliseconds;

        Period(long milliseconds) {
            this.milliseconds = milliseconds;
        }
    }

    /** Default: INCREASING */
    public enum Direction {
        DECREASING, INCREASING, NA
    }

    @JsonInclude(Include.NON_NULL)
    private String dataId;

    @DocModelProperty(description = "Indicate if a metric is increasing/decreasing.",
            position = 0,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private Direction direction;

    @DocModelProperty(description = "Time period used for the evaluation.",
            position = 1,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private Period period;

    @DocModelProperty(description = "Compare operator [LT (<), GT (>), LTE (<=), GTE (>=)].",
            position = 2,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private Operator operator;

    @DocModelProperty(description = "Condition threshold.",
            position = 3,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private Double threshold;

    /**
     * Used for JSON deserialization, not for general use.
     */
    public RateCondition() {
        /*
            Default constructor is needed for JSON libraries in JAX-RS context.
         */
        this("", "", Mode.FIRING, 1, 1, null, null, null, null, null);
    }

    public RateCondition(String tenantId, String triggerId, String dataId, Direction direction, Period period,
            Operator operator, Double threshold) {

        this(tenantId, triggerId, Mode.FIRING, 1, 1, dataId, direction, period, operator, threshold);
    }

    /**
     * This constructor requires the tenantId be assigned prior to persistence. It can be used when
     * creating triggers via Rest, as the tenant will be assigned automatically.
     */
    public RateCondition(String triggerId, Mode triggerMode, String dataId, Direction direction,
            Period period, Operator operator, Double threshold) {

        this("", triggerId, triggerMode, 1, 1, dataId, direction, period, operator, threshold);
    }

    public RateCondition(String tenantId, String triggerId, Mode triggerMode, String dataId, Direction direction,
            Period period, Operator operator, Double threshold) {

        this(tenantId, triggerId, triggerMode, 1, 1, dataId, direction, period, operator, threshold);
    }

    /**
     * This constructor requires the tenantId be assigned prior to persistence. It can be used when
     * creating triggers via Rest, as the tenant will be assigned automatically.
     */
    public RateCondition(String triggerId, Mode triggerMode, int conditionSetSize, int conditionSetIndex,
            String dataId, Direction direction, Period period, Operator operator, Double threshold) {

        this("", triggerId, triggerMode, conditionSetSize, conditionSetIndex, dataId, direction, period, operator,
                threshold);
    }

    public RateCondition(String tenantId, String triggerId, Mode triggerMode, int conditionSetSize,
            int conditionSetIndex,
            String dataId, Direction direction, Period period, Operator operator, Double threshold) {

        super(tenantId, triggerId, (null == triggerMode ? Mode.FIRING : triggerMode), conditionSetSize,
                conditionSetIndex, Type.RATE);
        this.dataId = dataId;
        this.direction = (null == direction) ? Direction.INCREASING : direction;
        this.period = (null == period) ? Period.MINUTE : period;
        this.operator = operator;
        this.threshold = threshold;
        updateDisplayString();
    }

    public RateCondition(RateCondition condition) {
        super(condition);

        this.dataId = condition.getDataId();
        this.direction = condition.getDirection();
        this.operator = condition.getOperator();
        this.period = condition.getPeriod();
        this.threshold = condition.getThreshold();
    }

    @Override
    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public boolean match(long time, double value, long previousTime, double previousValue) {
        double rate = getRate(time, value, previousTime, previousValue);

        if (rate < 0) {
            return false;
        }

        switch (operator) {
            case LT:
                return rate < threshold;
            case GT:
                return rate > threshold;
            case LTE:
                return rate <= threshold;
            case GTE:
                return rate >= threshold;
            default:
                throw new IllegalStateException("Unknown operator: " + operator.name());
        }
    }

    public double getRate(long time, double value, long previousTime, double previousValue) {
        long deltaTime = time - previousTime;
        double deltaValue = (Direction.INCREASING == direction) ? (value - previousValue) : (previousValue - value);
        double periods = deltaTime / (double)period.milliseconds;
        double rate = deltaValue / periods;

        return rate;
    }

    @Override
    public void updateDisplayString() {
        String direction = null == this.direction ? null : this.direction.name();
        String operator = null == this.operator ? null : this.operator.name();
        String period = null == this.period ? null : this.period.name();
        String s = String.format(Locale.US, "%s %s %s %.2f per %s", this.dataId, direction, operator, this.threshold,
                                 period);
        setDisplayString(s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RateCondition that = (RateCondition) o;
        return Objects.equals(dataId, that.dataId) &&
                direction == that.direction &&
                period == that.period &&
                operator == that.operator &&
                Objects.equals(threshold, that.threshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataId, direction, period, operator, threshold);
    }

    @Override
    public String toString() {
        return "RateCondition [dataId=" + dataId + ", direction=" + direction + ", period=" + period + ", operator="
                + operator + ", threshold=" + threshold + ", tenantId=" + tenantId + ", triggerId=" + triggerId
                + ", triggerMode=" + triggerMode + "]";
    }

}
