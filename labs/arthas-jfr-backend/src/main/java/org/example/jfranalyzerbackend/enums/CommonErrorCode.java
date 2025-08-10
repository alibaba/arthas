/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.example.jfranalyzerbackend.enums;


import org.example.jfranalyzerbackend.exception.ErrorCode;

public enum CommonErrorCode implements ErrorCode {
    ILLEGAL_ARGUMENT("Illegal argument"),

    VALIDATION_FAILURE("Validation failure"),

    INTERNAL_ERROR("Internal error");

    private final String message;

    CommonErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
