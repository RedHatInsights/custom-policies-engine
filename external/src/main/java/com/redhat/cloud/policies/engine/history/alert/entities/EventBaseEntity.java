package com.redhat.cloud.policies.engine.history.alert.entities;

import com.redhat.cloud.policies.engine.history.alert.converters.FactsConverter;
import com.redhat.cloud.policies.engine.history.alert.converters.ContextConverter;
import com.redhat.cloud.policies.engine.history.alert.converters.EvalSetsConverter;
import org.hawkular.alerts.api.model.condition.ConditionEval;
import org.hawkular.alerts.api.model.dampening.Dampening;
import org.hawkular.alerts.api.model.trigger.Trigger;

import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.DiscriminatorType.STRING;

@Entity
@Table(name = "events")
@DiscriminatorColumn(name = "discriminator", discriminatorType = STRING, length = 5)
public abstract class EventBaseEntity {

    @Id
    @GeneratedValue
    private UUID uuid;

    private LocalDateTime expiresAt;

    //@NotNull
    private String eventType;

    private String tenantId;

    //@NotNull
    private String id;

    private long ctime;

    private String datasource;

    //@NotNull
    private String dataId;

    //@NotNull
    private String category;

    //@NotNull
    @Lob // FIXME Temp, real field max length to be determined
    private String text;

    @Convert(converter = ContextConverter.class)
    @Lob // FIXME Temp, real field max length to be determined
    private Map<String, String> context;

    @OneToMany(mappedBy = "event", cascade = PERSIST)
    private Set<TagEntity> tags;

    // FIXME Check everything after that

    @Lob // FIXME Temp, real field max length to be determined
    private Trigger trigger;

    private String triggerId;

    @Lob // FIXME Temp, real field max length to be determined
    private Dampening dampening;

    @Convert(converter = EvalSetsConverter.class)
    @Lob // FIXME Temp, real field max length to be determined
    private List<Set<ConditionEval>> evalSets;

    @Convert(converter = FactsConverter.class)
    @Lob // FIXME Temp, real field max length to be determined
    private Map<String, Object> facts;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID id) {
        this.uuid = id;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getId() {
        return id;
    }

    public void setId(String eventId) {
        this.id = eventId;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public void setContext(Map<String, String> context) {
        this.context = context;
    }

    public Set<TagEntity> getTags() {
        return tags;
    }

    public void setTags(Set<TagEntity> tags) {
        this.tags = tags;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public String getTriggerId() {
        return triggerId;
    }

    public void setTriggerId(String triggerId) {
        this.triggerId = triggerId;
    }

    public Dampening getDampening() {
        return dampening;
    }

    public void setDampening(Dampening dampening) {
        this.dampening = dampening;
    }

    public List<Set<ConditionEval>> getEvalSets() {
        return evalSets;
    }

    public void setEvalSets(List<Set<ConditionEval>> evalSets) {
        this.evalSets = evalSets;
    }

    public Map<String, Object> getFacts() {
        return facts;
    }

    public void setFacts(Map<String, Object> facts) {
        this.facts = facts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof EventBaseEntity) {
            EventBaseEntity other = (EventBaseEntity) o;
            return Objects.equals(uuid, other.uuid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
