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

package org.example.jfranalyzerbackend.util;

public abstract class EventConstant {
    public static String UNSIGNED_INT_FLAG = "jdk.UnsignedIntFlag";
    public static String GARBAGE_COLLECTION = "jdk.GarbageCollection";

    public static String CPU_INFORMATION = "jdk.CPUInformation";
    public static String CPC_RUNTIME_INFORMATION = "cpc.RuntimeInformation";
    public static String ENV_VAR = "jdk.InitialEnvironmentVariable";

    public static String PROCESS_CPU_LOAD = "jdk.CPULoad";
    public static String ACTIVE_SETTING = "jdk.ActiveSetting";

    public static String THREAD_START = "jdk.ThreadStart";
    public static String THREAD_CPU_LOAD = "jdk.ThreadCPULoad";
    public static String EXECUTION_SAMPLE = "jdk.ExecutionSample";
    public static String WALL_CLOCK_SAMPLE = "jdk.ExecutionSample";
    public static String NATIVE_EXECUTION_SAMPLE = "jdk.NativeMethodSample";
    public static String EXECUTE_VM_OPERATION = "jdk.ExecuteVMOperation";

    public static String OBJECT_ALLOCATION_SAMPLE = "jdk.ObjectAllocationSample";
    public static String OBJECT_ALLOCATION_IN_NEW_TLAB = "jdk.ObjectAllocationInNewTLAB";
    public static String OBJECT_ALLOCATION_OUTSIDE_TLAB = "jdk.ObjectAllocationOutsideTLAB";

    public static String FILE_WRITE = "jdk.FileWrite";
    public static String FILE_READ = "jdk.FileRead";
    public static String FILE_FORCE = "jdk.FileForce";

    public static String SOCKET_READ = "jdk.SocketRead";
    public static String SOCKET_WRITE = "jdk.SocketWrite";

    public static String JAVA_MONITOR_ENTER = "jdk.JavaMonitorEnter";
    public static String JAVA_MONITOR_WAIT = "jdk.JavaMonitorWait";
    public static String THREAD_PARK = "jdk.ThreadPark";

    public static String CLASS_LOAD = "jdk.ClassLoad";

    public static String THREAD_SLEEP = "jdk.ThreadSleep";

    public static String PERIOD = "period";

    public static String INTERVAL = "interval";
    public static String WALL = "wall";
    public static String EVENT = "event";
}
