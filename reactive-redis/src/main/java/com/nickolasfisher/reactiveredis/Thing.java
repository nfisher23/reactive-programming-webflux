package com.nickolasfisher.reactiveredis;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Thing {
    private Integer id;
    private String value;
}
