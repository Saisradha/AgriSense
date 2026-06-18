package com.jo.agrisenseai;

/**
 * Represents the active field's summary information,
 * shown on the My Farm screen.
 */
public class FarmData {

    private String fieldName;
    private String fieldStatus;
    private String nextWatering;

    // Required empty constructor for Firebase
    public FarmData() {
    }

    public FarmData(String fieldName, String fieldStatus, String nextWatering) {
        this.fieldName = fieldName;
        this.fieldStatus = fieldStatus;
        this.nextWatering = nextWatering;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldStatus() {
        return fieldStatus;
    }

    public void setFieldStatus(String fieldStatus) {
        this.fieldStatus = fieldStatus;
    }

    public String getNextWatering() {
        return nextWatering;
    }

    public void setNextWatering(String nextWatering) {
        this.nextWatering = nextWatering;
    }
}
