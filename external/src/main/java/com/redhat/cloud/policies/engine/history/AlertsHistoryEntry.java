package com.redhat.cloud.policies.engine.history;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "alerts_history")
public class AlertsHistoryEntry {

    @Id
    @GeneratedValue
    private UUID id;

    private String tenantId;

    private String policyId;

    private long ctime;

    private String hostId;

    private String hostName;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof AlertsHistoryEntry) {
            AlertsHistoryEntry other = (AlertsHistoryEntry) o;
            return Objects.equals(id, other.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AlertsHistoryEntry [id=" + id + ", tenantId=" + tenantId + ", policyId=" + policyId + ", ctime=" + ctime + ", hostId=" + hostId + ", hostName=" + hostName + "]";
    }
}
