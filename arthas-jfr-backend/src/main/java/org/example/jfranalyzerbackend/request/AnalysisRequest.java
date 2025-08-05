/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.example.jfranalyzerbackend.request;

import lombok.Getter;

import java.io.InputStream;
import java.nio.file.Path;

@Getter
public class AnalysisRequest {
    private final int parallelWorkers;
    private final Path input;
    private final InputStream inputStream;
    private final int dimensions;

    public AnalysisRequest(Path input, int dimensions) {
        this(1, input, dimensions);
    }

    public AnalysisRequest(InputStream stream, int dimensions) {
        this(1, null, stream, dimensions);
    }

    public AnalysisRequest(int parallelWorkers, Path input, int dimensions) {
        this(parallelWorkers, input, null, dimensions);
    }

    private AnalysisRequest(int parallelWorkers, Path p, InputStream stream, int dimensions) {
        this.parallelWorkers = parallelWorkers;
        this.input = p;
        this.dimensions = dimensions;
        this.inputStream = stream;
    }
}
