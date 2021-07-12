package com.redhat.cloud.policies.engine.history.alert.entities;

import com.redhat.cloud.policies.engine.history.alert.converters.ConditionEvalsConverter;
import com.redhat.cloud.policies.engine.history.alert.converters.LifecyclesConverter;
import org.hawkular.alerts.api.model.Lifecycle;
import org.hawkular.alerts.api.model.Severity;
import org.hawkular.alerts.api.model.condition.ConditionEval;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import java.util.List;
import java.util.Set;

import static javax.persistence.EnumType.STRING;
import static org.hawkular.alerts.api.model.event.Alert.Status;

@Entity
@DiscriminatorValue("alert")
public class AlertEntity extends EventBaseEntity {

    @Enumerated(STRING)
    @Column(length = 8) // TODO Remove after the SQL script is stabilized
    private Severity severity;

    @Enumerated(STRING)
    @Column(length = 12) // TODO Remove after the SQL script is stabilized
    private Status status;

    @Convert(converter = LifecyclesConverter.class)
    @Lob // FIXME Temp, real field max length to be determined
    private List<Lifecycle> lifecycle;

    private Long stime;

    @Convert(converter = ConditionEvalsConverter.class)
    @Lob // TODO Remove after the SQL script is stabilized
    private List<Set<ConditionEval>> resolvedEvalSets;

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<Lifecycle> getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(List<Lifecycle> lifecycle) {
        this.lifecycle = lifecycle;
    }

    public Long getStime() {
        return stime;
    }

    public void setStime(Long stime) {
        this.stime = stime;
    }

    public List<Set<ConditionEval>> getResolvedEvalSets() {
        return resolvedEvalSets;
    }

    public void setResolvedEvalSets(List<Set<ConditionEval>> resolvedEvalSets) {
        this.resolvedEvalSets = resolvedEvalSets;
    }
}
