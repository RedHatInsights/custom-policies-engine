package com.redhat.cloud.policies.engine.history.alert.entities;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("event")
public class EventEntity extends EventBaseEntity {
}
