package com.jo.agrisenseai;

/**
 * Represents a farmer's profile stored at {@code Users/{uid}/profile/} in Firebase.
 * Collected during the registration flow after phone authentication.
 */
public class UserProfile {

    private String uid;
    private String name;
    private String phone;
    private String village;
    private String district;
    private String state;
    private boolean hasDevice;
    private boolean demoMode;
    private long registeredAt;

    // Required empty constructor for Firebase
    public UserProfile() {
    }

    public UserProfile(String uid, String name, String phone, String village,
                       String district, String state, boolean hasDevice,
                       boolean demoMode, long registeredAt) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.village = village;
        this.district = district;
        this.state = state;
        this.hasDevice = hasDevice;
        this.demoMode = demoMode;
        this.registeredAt = registeredAt;
    }

    // ── Getters ──────────────────────────────────────────────────────────
    public String getUid()          { return uid; }
    public String getName()         { return name; }
    public String getPhone()        { return phone; }
    public String getVillage()      { return village; }
    public String getDistrict()     { return district; }
    public String getState()        { return state; }
    public boolean isHasDevice()    { return hasDevice; }
    public boolean isDemoMode()     { return demoMode; }
    public long getRegisteredAt()   { return registeredAt; }

    // ── Setters ──────────────────────────────────────────────────────────
    public void setUid(String uid)              { this.uid = uid; }
    public void setName(String name)            { this.name = name; }
    public void setPhone(String phone)          { this.phone = phone; }
    public void setVillage(String village)       { this.village = village; }
    public void setDistrict(String district)     { this.district = district; }
    public void setState(String state)          { this.state = state; }
    public void setHasDevice(boolean hasDevice)  { this.hasDevice = hasDevice; }
    public void setDemoMode(boolean demoMode)    { this.demoMode = demoMode; }
    public void setRegisteredAt(long registeredAt) { this.registeredAt = registeredAt; }
}
