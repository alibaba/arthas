#!/usr/bin/env bash

# WIKI: https://alibaba.github.io/arthas
# This script only supports bash, do not support posix sh.
# If you have the problem like Syntax error: "(" unexpected (expecting "fi"),
# Try to run "bash -version" to check the version.
# Try to visit WIKI to find a solution.

# program : Arthas
#  author : Core Engine @ Taobao.com
#    date : 2018-09-17

# current arthas script version
ARTHAS_SCRIPT_VERSION=3.0.4

# define arthas's home
ARTHAS_HOME=${HOME}/.arthas

# define arthas's lib
ARTHAS_LIB_DIR=${ARTHAS_HOME}/lib

# define arthas's temp dir
TMP_DIR=/tmp

# last update arthas version
ARTHAS_VERSION=

# arthas remote url
ARTHAS_REMOTE_VERSION_URL="http://search.maven.org/solrsearch/select?q=g:%22com.taobao.arthas%22+AND+a:%22arthas-packaging%22"
ARTHAS_REMOTE_DOWNLOAD_URL="http://search.maven.org/classic/remotecontent?filepath=com/taobao/arthas/arthas-packaging"

# update timeout(sec)
SO_TIMEOUT=5

# define default target ip
DEFAULT_TARGET_IP="127.0.0.1"

# define default target port
DEFAULT_TELNET_PORT="3658"
DEFAULT_HTTP_PORT="8563"

# define JVM's OPS
JVM_OPTS=""

# define default batch mode
BATCH_MODE=false

# if true, the script will only attach the agent to target jvm.
ATTACH_ONLY=false

# define batch script location
BATCH_SCRIPT=

ARTHAS_OPTS="-Djava.awt.headless=true"

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}


# get with default value
# $1 : target value
# $2 : default value
default()
{
    [[ ! -z "${1}" ]] && echo "${1}" || echo "${2}"
}


# check arthas permission
check_permission()
{
    [ ! -w ${HOME} ] \
        && exit_on_err 1 "permission denied, ${HOME} is not writeable."
}


# reset arthas work environment
# reset some options for env
reset_for_env()
{

    # init ARTHAS' lib
    mkdir -p ${ARTHAS_LIB_DIR} \
        || exit_on_err 1 "create ${ARTHAS_LIB_DIR} fail."

    # if env define the JAVA_HOME, use it first
    # if is alibaba opts, use alibaba ops's default JAVA_HOME
    [ -z ${JAVA_HOME} ] && JAVA_HOME=/opt/taobao/java

    # iterater throught candidates to find a proper JAVA_HOME at least contains tools.jar which is required by arthas.
    if [ ! -d ${JAVA_HOME} ]; then
        JAVA_HOME_CANDIDATES=($(ps aux | grep java | grep -v 'grep java' | awk '{print $11}' | sed -n 's/\/bin\/java$//p'))
        for JAVA_HOME_TEMP in ${JAVA_HOME_CANDIDATES[@]}; do
            if [ -f ${JAVA_HOME_TEMP}/lib/tools.jar ]; then
                JAVA_HOME=${JAVA_HOME_TEMP}
                break
            fi
        done
    fi

    # maybe 1.8.0_162 , 11-ea
    local JAVA_VERSION_STR=$(${JAVA_HOME}/bin/java -version 2>&1|awk -F '"' '$2>"1.5"{print $2}')
    # check the jvm version, we need 1.6+
    [[ ! -x ${JAVA_HOME} || -z ${JAVA_VERSION_STR} ]] && exit_on_err 1 "illegal ENV, please set \$JAVA_HOME to JDK6+"

    local JAVA_VERSION
    if [[ $JAVA_VERSION_STR = "1."* ]]; then
        JAVA_VERSION=$(echo $veJAVA_VERSION_STRr | sed -e 's/1\.\([0-9]*\)\(.*\)/\1/; 1q')
    else
        JAVA_VERSION=$(echo $JAVA_VERSION_STR | sed -e 's/\([0-9]*\)\(.*\)/\1/; 1q')
    fi

    # when java version greater than 9, there is no tools.jar
    if [[ "$JAVA_VERSION" -lt 9 ]];then
      # check tools.jar exists
      if [ ! -f ${JAVA_HOME}/lib/tools.jar ]; then
          exit_on_err 1 "${JAVA_HOME}/lib/tools.jar does not exist, arthas could not be launched!"
      else
          BOOT_CLASSPATH=-Xbootclasspath/a:${JAVA_HOME}/lib/tools.jar
      fi
    fi

    # reset CHARSET for alibaba opts, we use GBK
    [[ -x /opt/taobao/java ]] && JVM_OPTS="-Dinput.encoding=GBK ${JVM_OPTS} "

}

# get latest version from local
get_local_version()
{
    ls ${ARTHAS_LIB_DIR} | sort | tail -1
}

# get latest version from remote
get_remote_version()
{
    curl -sLk --connect-timeout ${SO_TIMEOUT} "${ARTHAS_REMOTE_VERSION_URL}" | sed 's/{.*latestVersion":"*\([0-9a-zA-Z\\.\\-]*\)"*,*.*}/\1/'
}

# make version format to comparable format like 000.000.(0){15}
# $1 : version
to_comparable_version()
{
    echo ${1}|awk -F "." '{printf("%d.%d.%d\n",$1,$2,$3)}'
}

# update arthas if necessary
update_if_necessary()
{
    local update_version=$1

    if [ ! -d ${ARTHAS_LIB_DIR}/${update_version} ]; then
        echo "updating version ${update_version} ..."

        local temp_target_lib_dir="$TMP_DIR/temp_${update_version}_$$"
        local temp_target_lib_zip="${temp_target_lib_dir}/arthas-${update_version}-bin.zip"
        local target_lib_dir="${ARTHAS_LIB_DIR}/${update_version}/arthas"
        mkdir -p ${target_lib_dir}

        # clean
        rm -rf ${temp_target_lib_dir}
        rm -rf ${target_lib_dir}

        mkdir -p "${temp_target_lib_dir}" \
            || exit_on_err 1 "create ${temp_target_lib_dir} fail."

        # download current arthas version
        curl \
            -#Lk \
            --connect-timeout ${SO_TIMEOUT} \
            -o ${temp_target_lib_zip} \
            "${ARTHAS_REMOTE_DOWNLOAD_URL}/${update_version}/arthas-packaging-${update_version}-bin.zip"  \
        || return 1

        # unzip arthas lib
        unzip ${temp_target_lib_zip} -d ${temp_target_lib_dir} || (rm -rf ${temp_target_lib_dir} && return 1)

        # rename
        mv ${temp_target_lib_dir} ${target_lib_dir} || return 1

        # print success
        echo "update completed."
    fi
}

# the usage
usage()
{
    echo "
Usage:
    $0 [-b [-f SCRIPT_FILE]] [debug] [--use-version VERSION] [--attach-only] <PID>[@IP:TELNET_PORT:HTTP_PORT]
    [debug]         : start the agent in debug mode
    <PID>           : the target Java Process ID
    [IP]            : the target's IP
    [TELNET_PORT]   : the target's PORT for telnet
    [HTTP_PORT]     : the target's PORT for http
    [-b]            : batch mode, which will disable interactive process selection.
    [-f]            : specify the path to batch script file.
    [--attach-only] : only attach the arthas agent to target jvm.
    [--use-version] : use the specified arthas version to attach.

Example:
    ./as.sh <PID>
    ./as.sh <PID>@[IP]
    ./as.sh <PID>@[IP:PORT]
    ./as.sh debug <PID>
    ./as.sh -b <PID>
    ./as.sh -b -f /path/to/script
    ./as.sh --attach-only <PID>
    ./as.sh --use-version 2.0.20161221142407 <PID>

Here is the list of possible java process(es) to attatch:

$(${JAVA_HOME}/bin/jps -l | grep -v sun.tools.jps.Jps)
"
}

# parse the argument
parse_arguments()
{
    if ([ "$1" = "-h" ] || [ "$1" = "--help" ] || [ "$1" = "-help" ]) ; then
        usage
        exit 0
    fi

    if [ "$1" = "-b" ]; then
       BATCH_MODE=true
       shift
       if [ "$1" = "-f" ]; then
           if [ "x$2" != "x" ] && [ -f $2 ]; then
               BATCH_SCRIPT=$2
               echo "Using script file for batch mode: $BATCH_SCRIPT"
               shift # -f
               shift # /path/to/script
           else
               echo "Invalid script file $2."
               return 1
           fi
        fi
    fi

    if [ "$1" = "debug" ] ; then
      if [ -z "$JPDA_TRANSPORT" ]; then
        JPDA_TRANSPORT="dt_socket"
      fi
      if [ -z "$JPDA_ADDRESS" ]; then
        JPDA_ADDRESS="8888"
      fi
      if [ -z "$JPDA_SUSPEND" ]; then
        JPDA_SUSPEND="n"
      fi
      if [ -z "$JPDA_OPTS" ]; then
        JPDA_OPTS="-agentlib:jdwp=transport=$JPDA_TRANSPORT,address=$JPDA_ADDRESS,server=y,suspend=$JPDA_SUSPEND"
      fi
      ARTHAS_OPTS="$JPDA_OPTS $ARTHAS_OPTS"
      shift
    fi

    # use custom version
    if [ "$1" = "--use-version" ]; then
      shift
      ARTHAS_VERSION=$1
      shift
    fi

    # attach only mode
    if [ "$1" = "--attach-only" ]; then
      ATTACH_ONLY=true
      shift
    fi

    TARGET_PID=$(echo ${1}|awk -F "@"   '{print $1}');
    TARGET_IP=$(echo ${1}|awk -F "@|:" '{print $2}');
    TELNET_PORT=$(echo ${1}|awk -F ":"   '{print $2}');
    HTTP_PORT=$(echo ${1}|awk -F ":"   '{print $3}');

    # check pid
    if [ -z ${TARGET_PID} ] && [ ${BATCH_MODE} = false ]; then
        # interactive mode
        IFS=$'\n'
        CANDIDATES=($(${JAVA_HOME}/bin/jps -l | grep -v sun.tools.jps.Jps | awk '{print $0}'))

        if [ ${#CANDIDATES[@]} -eq 0 ]; then
            echo "Error: no available java process to attach."
            return 1
        fi

        echo "Found existing java process, please choose one and hit RETURN."

        index=0
        suggest=1
        # auto select tomcat/pandora-boot process
        for process in "${CANDIDATES[@]}"; do
            index=$(($index+1))
            if [ $(echo ${process} | grep -c org.apache.catalina.startup.Bootstrap) -eq 1 ] \
                || [ $(echo ${process} | grep -c com.taobao.pandora.boot.loader.SarLauncher) -eq 1 ]
            then
               suggest=${index}
               break
            fi
        done

        index=0
        for process in "${CANDIDATES[@]}"; do
            index=$(($index+1))
            if [ ${index} -eq ${suggest} ]; then
                echo "* [$index]: ${process}"
            else
                echo "  [$index]: ${process}"
            fi
        done

        read choice

        if [ -z ${choice} ]; then
            choice=${suggest}
        fi

        TARGET_PID=`echo ${CANDIDATES[$(($choice-1))]} | cut -d ' ' -f 1`

    elif [ -z ${TARGET_PID} ]; then
        # batch mode is enabled, no interactive process selection.
        echo "Illegal arguments, the <PID> is required." 1>&2
        return 1
    fi

    # reset ${ip} to default if empty
    [ -z ${TARGET_IP} ] && TARGET_IP=${DEFAULT_TARGET_IP}

    # reset ${port} to default if empty
    [ -z ${TELNET_PORT} ] && TELNET_PORT=${DEFAULT_TELNET_PORT}
    [ -z ${HTTP_PORT} ] && HTTP_PORT=${DEFAULT_HTTP_PORT}

    return 0

}


# attach arthas to target jvm
# $1 : arthas_local_version
attach_jvm()
{
    local arthas_version=$1
    local arthas_lib_dir=${ARTHAS_LIB_DIR}/${arthas_version}/arthas

    echo "Attaching to ${TARGET_PID} using version ${1}..."

    if [ ${TARGET_IP} = ${DEFAULT_TARGET_IP} ]; then
        if [[ "${arthas_version}" > "3.0" ]]; then
            ${JAVA_HOME}/bin/java \
                ${ARTHAS_OPTS} ${BOOT_CLASSPATH} ${JVM_OPTS} \
                -jar ${arthas_lib_dir}/arthas-core.jar \
                    -pid ${TARGET_PID} \
                    -target-ip ${TARGET_IP} \
                    -telnet-port ${TELNET_PORT} \
                    -http-port ${HTTP_PORT} \
                    -core "${arthas_lib_dir}/arthas-core.jar" \
                    -agent "${arthas_lib_dir}/arthas-agent.jar"
        else
            # for compatibility
            ${JAVA_HOME}/bin/java \
                ${ARTHAS_OPTS} ${BOOT_CLASSPATH} ${JVM_OPTS} \
                -jar ${arthas_lib_dir}/arthas-core.jar \
                    -pid ${TARGET_PID} \
                    -target ${TARGET_IP}":"${TELNET_PORT} \
                    -core "${arthas_lib_dir}/arthas-core.jar" \
                    -agent "${arthas_lib_dir}/arthas-agent.jar"

            # verify_pid
            echo "help" > /tmp/command
            PID=`${JAVA_HOME}/bin/java -cp ${arthas_lib_dir}/arthas-core.jar ${ARTHAS_OPTS}\
                com.taobao.arthas.core.ArthasConsole ${TARGET_IP} ${TELNET_PORT} -b -f /tmp/command \
                | grep PID | awk '{print $2}'`
            rm /tmp/command
            if [ ! -z ${PID} ] && [ "${PID}" != "${TARGET_PID}" ]; then
                echo "WARNING: Arthas server is running on ${PID} instead of ${TARGET_PID}, exiting."
                exit 1
            fi
        fi
    fi
}

sanity_check() {
    # 0 check whether the pid exist
    local pid=$(ps -p ${TARGET_PID} -o pid=)
    if [ -z ${pid} ]; then
        exit_on_err 1 "The target pid (${TARGET_PID}) does not exist!"
    fi

    # 1 check the current user matches the process owner
    local current_user=$(id -u -n)
    # the last '=' after 'user' eliminates the column header
    local target_user=$(ps -p "${TARGET_PID}" -o user=)
    if [ "$current_user" != "$target_user" ]; then
        echo "The current user ($current_user) does not match with the owner of process ${TARGET_PID} ($target_user)."
        echo "To solve this, choose one of the following command:"
        echo "  1) sudo su $target_user && ./as.sh"
        echo "  2) sudo -u $target_user -EH ./as.sh"
        exit_on_err 1
    fi
}

# active console
# $1 : arthas_local_version
active_console()
{
    local arthas_version=$1
    local arthas_lib_dir=${ARTHAS_LIB_DIR}/${arthas_version}/arthas

    if [[ "${arthas_version}" > "3.0" ]]; then
        if [ "${BATCH_MODE}" = "true" ]; then
            ${JAVA_HOME}/bin/java ${ARTHAS_OPTS} ${JVM_OPTS} \
                 -jar ${arthas_lib_dir}/arthas-client.jar \
                 ${TARGET_IP} \
                 -p ${TELNET_PORT} \
                 -f ${BATCH_SCRIPT}
        elif type telnet 2>&1 >> /dev/null; then
            # use telnet
            telnet ${TARGET_IP} ${TELNET_PORT}
        else
            echo "'telnet' is required." 1>&2
            return 1
        fi
    else
        # for compatibility
        # use default console
        ARGS="${TARGET_IP} ${TELNET_PORT}"
        if [ ${BATCH_MODE} = true ]; then
            ARGS="$ARGS -b"
        fi
        if [ ! -z ${BATCH_SCRIPT} ]; then
            ARGS="$ARGS -f $BATCH_SCRIPT"
        fi
        eval ${JAVA_HOME}/bin/java ${ARTHAS_OPTS} \
            -cp ${arthas_lib_dir}/arthas-core.jar \
            com.taobao.arthas.core.ArthasConsole \
            ${ARGS}
    fi
}

# the main
main()
{
    echo "Arthas script version: $ARTHAS_SCRIPT_VERSION"

    check_permission
    reset_for_env

    parse_arguments "${@}" \
        || exit_on_err 1 "$(usage)"

    local remote_version=$(get_remote_version)

    if [ -z ${ARTHAS_VERSION} ]; then
        update_if_necessary ${remote_version} || echo "update fail, ignore this update." 1>&2
    else
        update_if_necessary ${ARTHAS_VERSION} || echo "update fail, ignore this update." 1>&2
    fi

    local arthas_local_version=$(get_local_version)

    if [ ! -z ${ARTHAS_VERSION} ]; then
        arthas_local_version=${ARTHAS_VERSION}
    fi

    if [ ! -d ${ARTHAS_LIB_DIR}/${arthas_local_version} ]; then
        exit_on_err 1 "arthas not found, please check your network."
    fi

    sanity_check

    echo "Calculating attach execution time..."
    time (attach_jvm ${arthas_local_version} || exit 1)

    if [ $? -ne 0 ]; then
        exit_on_err 1 "attach to target jvm (${TARGET_PID}) failed, check ${HOME}/logs/arthas/arthas.log or stderr of target jvm for any exceptions."
    fi

    echo "Attach success."

    if [ ${ATTACH_ONLY} = false ]; then
      echo "Connecting to arthas server... current timestamp is `date +%s`"
      active_console ${arthas_local_version}
    fi
}



main "${@}"
