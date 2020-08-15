package com.nickolasfisher.testing.dto;

import java.util.List;

public class DownstreamResponseDTO {
    private String firstName;
    private String lastName;
    private String ssn;
    private String deepesetFear;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public String getDeepesetFear() {
        return deepesetFear;
    }

    public void setDeepesetFear(String deepesetFear) {
        this.deepesetFear = deepesetFear;
    }
}
