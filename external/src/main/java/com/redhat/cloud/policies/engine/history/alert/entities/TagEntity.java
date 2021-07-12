package com.redhat.cloud.policies.engine.history.alert.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static javax.persistence.CascadeType.PERSIST;

@Entity
@Table(name = "tags")
public class TagEntity {

    @Id
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "event_uuid")
    private EventBaseEntity event;

    private String key;

    @OneToMany(mappedBy = "tag", cascade = PERSIST)
    private Set<TagValueEntity> values;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID id) {
        this.uuid = id;
    }

    public EventBaseEntity getEvent() {
        return event;
    }

    public void setEvent(EventBaseEntity event) {
        this.event = event;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Set<TagValueEntity> getValues() {
        return values;
    }

    public void setValues(Set<TagValueEntity> values) {
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof TagEntity) {
            TagEntity other = (TagEntity) o;
            return Objects.equals(uuid, other.uuid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
