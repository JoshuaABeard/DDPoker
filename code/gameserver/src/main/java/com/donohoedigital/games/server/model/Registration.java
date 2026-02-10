/*
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 * Licensed under GPL-3.0
 *
 * Stub class for backward compatibility.
 * Registration tracking removed in Community Edition (open source).
 */
package com.donohoedigital.games.server.model;

import com.donohoedigital.comms.Version;
import com.donohoedigital.db.model.BaseModel;
import java.util.Date;

/**
 * @deprecated Registration tracking removed in open source version.
 * This stub exists only for compilation compatibility.
 */
@Deprecated
public class Registration implements BaseModel<Long> {

    /**
     * @deprecated Registration Type enum stub for compilation
     */
    @Deprecated
    public enum Type {
        UNKNOWN, RETAIL, ONLINE, DEMO, REGISTRATION, PATCH, ACTIVATION
    }

    // Stub fields
    private Long id;
    private Type type = Type.UNKNOWN;
    private String name;
    private String email;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postal;
    private String operatingSystem;
    private String javaVersion;
    private String licenseKey;
    private String version;
    private Date serverTime;
    private String ip;
    private Integer port;
    private String hostName;
    private String hostNameModified;
    private boolean banAttempt;
    private boolean duplicate;

    // Stub methods for compilation compatibility
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPostal() { return postal; }
    public void setPostal(String postal) { this.postal = postal; }

    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String os) { this.operatingSystem = os; }

    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String version) { this.javaVersion = version; }

    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String key) { this.licenseKey = key; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public void setVersion(Version version) { this.version = version != null ? version.toString() : null; }

    public Date getServerTime() { return serverTime; }
    public void setServerTime(Date time) { this.serverTime = time; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public String getHostName() { return hostName; }
    public void setHostName(String name) { this.hostName = name; }

    public String getHostNameModified() { return hostNameModified; }
    public void setHostNameModified(String name) { this.hostNameModified = name; }

    public boolean isBanAttempt() { return banAttempt; }
    public void setBanAttempt(boolean ban) { this.banAttempt = ban; }

    public boolean isDuplicate() { return duplicate; }
    public void setDuplicate(boolean dup) { this.duplicate = dup; }

    public boolean isRegistration() { return type == Type.REGISTRATION; }
    public boolean isActivation() { return type == Type.ACTIVATION; }
    public boolean isPatch() { return type == Type.PATCH; }

    public long getServerTimeMillis() {
        return serverTime != null ? serverTime.getTime() : 0L;
    }

    public boolean isLinux() {
        return operatingSystem != null && operatingSystem.toLowerCase().contains("linux");
    }

    public boolean isMac() {
        return operatingSystem != null && operatingSystem.toLowerCase().contains("mac");
    }

    public boolean isWin() {
        return operatingSystem != null && operatingSystem.toLowerCase().contains("win");
    }

    /**
     * Utility method for generifying hostname (stub implementation)
     */
    public static String generifyHostName(String hostname) {
        return hostname;
    }
}
