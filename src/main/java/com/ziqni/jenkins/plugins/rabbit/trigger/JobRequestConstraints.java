/*
 * Copyright (c) 2024. ZIQNI LTD registered in England and Wales, company registration number-09693684
 */

package com.ziqni.jenkins.plugins.rabbit.trigger;

public enum JobRequestConstraints {

    CONFIRM_RECEIPT("confirm_receipt");

    private final String value;

    JobRequestConstraints(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
