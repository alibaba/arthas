#!/usr/bin/env bash

# WIKI: https://alibaba.github.io/arthas
# This script only supports bash, do not support posix sh.
# If you have the problem like Syntax error: "(" unexpected (expecting "fi"),
# Try to run "bash -version" to check the version.
# Try to visit WIKI to find a solution.

# program : Arthas
#  author : Core Engine @ Taobao.com
#    date : 2018-11-28

# current arthas script version
ARTHAS_SCRIPT_VERSION=3.0.5

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

############ Command Arguments ############

# define arthas's home
ARTHAS_HOME=

# define arthas's lib
ARTHAS_LIB_DIR=${HOME}/.arthas/lib

# target process id to attach
TARGET_PID=

# target process id to attach
TARGET_IP="127.0.0.1"

# telnet port
TELNET_PORT="3658"

# http port
HTTP_PORT="8563"

# telnet session timeout seconds, default 300
SESSION_TIMEOUT=300

# use specify version
USE_VERSION=

# maven repo to download arthas
REPO_MIRROR="center"

# use http to download arthas
USE_HTTP=false

# attach only, do not telnet connect
ATTACH_ONLY=false

# pass debug arguments to the attach java process
DEBUG_ATTACH=false

# Verbose, print debug info.
VERBOSE=false

# command to execute
COMMAND=
# batch file to execute
BATCH_FILE=

############ Command Arguments ############

# if arguments contains -c/--command or -f/--batch-file,  BATCH_MODE will be true
BATCH_MODE=false

# define arthas's temp dir
TMP_DIR=/tmp

# last update arthas version
ARTHAS_VERSION=

# maven-metadata.xml url
# https://repo1.maven.org/maven2/com/taobao/arthas/arthas-packaging/maven-metadata.xml
MAVEN_METADATA_URL="PLACEHOLDER_REPO/com/taobao/arthas/arthas-packaging/maven-metadata.xml"
# arthas remote url
# https://repo1.maven.org/maven2/com/taobao/arthas/arthas-packaging/3.x.x/arthas-packaging-3.x.x-bin.zip
REMOTE_DOWNLOAD_URL="PLACEHOLDER_REPO/com/taobao/arthas/arthas-packaging/PLACEHOLDER_VERSION/arthas-packaging-PLACEHOLDER_VERSION-bin.zip"

# update timeout(sec)
SO_TIMEOUT=5

# define JVM's OPS
JVM_OPTS=""

ARTHAS_OPTS="-Djava.awt.headless=true"

OS_TYPE=
case "$(uname -s)" in
    Linux*)     OS_TYPE=Linux;;
    Darwin*)    OS_TYPE=Mac;;
    CYGWIN*)    OS_TYPE=Cygwin;;
    MINGW*)     OS_TYPE=MinGw;;
    *)          OS_TYPE="UNKNOWN"
esac

# check curl/grep/awk/telent/unzip command
if ! [ -x "$(command -v curl)" ]; then
  echo 'Error: curl is not installed. Try to use java -jar arthas-boot.jar' >&2
  exit 1
fi
if ! [ -x "$(command -v grep)" ]; then
  echo 'Error: grep is not installed. Try to use java -jar arthas-boot.jar' >&2
  exit 1
fi
if ! [ -x "$(command -v awk)" ]; then
  echo 'Error: awk is not installed. Try to use java -jar arthas-boot.jar' >&2
  exit 1
fi
if ! [ -x "$(command -v telnet)" ]; then
  echo 'Error: telnet is not installed. Try to use java -jar arthas-boot.jar' >&2
  exit 1
fi
if ! [ -x "$(command -v unzip)" ]; then
  echo 'Error: unzip is not installed. Try to use java -jar arthas-boot.jar' >&2
  exit 1
fi

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
    [ ! -w "${HOME}" ] \
        && exit_on_err 1 "permission denied, ${HOME} is not writable."
}


# SYNOPSIS
#   rreadlink <fileOrDirPath>
# DESCRIPTION
#   Resolves <fileOrDirPath> to its ultimate target, if it is a symlink, and
#   prints its canonical path. If it is not a symlink, its own canonical path
#   is printed.
#   A broken symlink causes an error that reports the non-existent target.
# LIMITATIONS
#   - Won't work with filenames with embedded newlines or filenames containing 
#     the string ' -> '.
# COMPATIBILITY
#   This is a fully POSIX-compliant implementation of what GNU readlink's
#    -e option does.
# EXAMPLE
#   In a shell script, use the following to get that script's true directory of origin:
#     trueScriptDir=$(dirname -- "$(rreadlink "$0")")
rreadlink() ( # Execute the function in a *subshell* to localize variables and the effect of `cd`.

  target=$1 fname= targetDir= CDPATH=

  # Try to make the execution environment as predictable as possible:
  # All commands below are invoked via `command`, so we must make sure that
  # `command` itself is not redefined as an alias or shell function.
  # (Note that command is too inconsistent across shells, so we don't use it.)
  # `command` is a *builtin* in bash, dash, ksh, zsh, and some platforms do not 
  # even have an external utility version of it (e.g, Ubuntu).
  # `command` bypasses aliases and shell functions and also finds builtins 
  # in bash, dash, and ksh. In zsh, option POSIX_BUILTINS must be turned on for
  # that to happen.
  { \unalias command; \unset -f command; } >/dev/null 2>&1
  [ -n "$ZSH_VERSION" ] && options[POSIX_BUILTINS]=on # make zsh find *builtins* with `command` too.

  while :; do # Resolve potential symlinks until the ultimate target is found.
      [ -L "$target" ] || [ -e "$target" ] || { command printf '%s\n' "ERROR: '$target' does not exist." >&2; return 1; }
      command cd "$(command dirname -- "$target")" # Change to target dir; necessary for correct resolution of target path.
      fname=$(command basename -- "$target") # Extract filename.
      [ "$fname" = '/' ] && fname='' # !! curiously, `basename /` returns '/'
      if [ -L "$fname" ]; then
        # Extract [next] target path, which may be defined
        # *relative* to the symlink's own directory.
        # Note: We parse `ls -l` output to find the symlink target
        #       which is the only POSIX-compliant, albeit somewhat fragile, way.
        target=$(command ls -l "$fname")
        target=${target#* -> }
        continue # Resolve [next] symlink target.
      fi
      break # Ultimate target reached.
  done
  targetDir=$(command pwd -P) # Get canonical dir. path
  # Output the ultimate target's canonical path.
  # Note that we manually resolve paths ending in /. and /.. to make sure we have a normalized path.
  if [ "$fname" = '.' ]; then
    command printf '%s\n' "${targetDir%/}"
  elif  [ "$fname" = '..' ]; then
    # Caveat: something like /var/.. will resolve to /private (assuming /var@ -> /private/var), i.e. the '..' is applied
    # AFTER canonicalization.
    command printf '%s\n' "$(command dirname -- "${targetDir}")"
  else
    command printf '%s\n' "${targetDir%/}/$fname"
  fi
)

# reset arthas work environment
# reset some options for env
reset_for_env()
{

    # init ARTHAS' lib
    mkdir -p "${ARTHAS_LIB_DIR}" \
        || exit_on_err 1 "create ${ARTHAS_LIB_DIR} fail."

    # if env define the JAVA_HOME, use it first
    # if is alibaba opts, use alibaba ops's default JAVA_HOME
    [ -z "${JAVA_HOME}" ] && [ -d /opt/taobao/java ] && JAVA_HOME=/opt/taobao/java

    if [[ (-z "${JAVA_HOME}") && ( -e "/usr/libexec/java_home") ]]; then
        # for mac
        JAVA_HOME=`/usr/libexec/java_home`
    fi

    if [ -z "${JAVA_HOME}" ]; then
        # try to find JAVA_HOME from java command
        local JAVA_COMMAND_PATH=$( rreadlink $(type -p java) )
        JAVA_HOME=$(echo "$JAVA_COMMAND_PATH" | sed -n 's/\/bin\/java$//p')
    fi

    # iterater throught candidates to find a proper JAVA_HOME at least contains tools.jar which is required by arthas.
    if [ ! -d "${JAVA_HOME}" ]; then
        JAVA_HOME_CANDIDATES=($(ps aux | grep java | grep -v 'grep java' | awk '{print $11}' | sed -n 's/\/bin\/java$//p'))
        for JAVA_HOME_TEMP in ${JAVA_HOME_CANDIDATES[@]}; do
            if [ -f "${JAVA_HOME_TEMP}/lib/tools.jar" ]; then
                JAVA_HOME=`rreadlink "${JAVA_HOME_TEMP}"`
                break
            fi
        done
    fi

    if [ -z "${JAVA_HOME}" ]; then
        exit_on_err 1 "Can not find JAVA_HOME, please set \$JAVA_HOME bash env first."
    fi

    # when java version less than 9, we can use tools.jar to confirm java home.
    # when java version greater than 9, there is no tools.jar.
    if [[ "$JAVA_VERSION" -lt 9 ]];then
      # possible java homes
      javaHomes=("${JAVA_HOME%%/}" "${JAVA_HOME%%/}/.." "${JAVA_HOME%%/}/../..")
      for javaHome in ${javaHomes[@]}
      do
          toolsJar="$javaHome/lib/tools.jar"
          if [ -f $toolsJar ]; then
              JAVA_HOME=$( rreadlink $javaHome )
              BOOT_CLASSPATH=-Xbootclasspath/a:$( rreadlink $toolsJar )
              break
          fi
      done
      [ -z "${BOOT_CLASSPATH}" ] && exit_on_err 1 "tools.jar was not found, so arthas could not be launched!"
    fi

    echo "[INFO] JAVA_HOME: ${JAVA_HOME}"

    # maybe 1.8.0_162 , 11-ea
    local JAVA_VERSION

    local IFS=$'\n'
    # remove \r for Cygwin
    local lines=$("${JAVA_HOME}"/bin/java -version 2>&1 | tr '\r' '\n')
    for line in $lines; do
      if [[ (-z $JAVA_VERSION) && ($line = *"version \""*) ]]
      then
        local ver=$(echo $line | sed -e 's/.*version "\(.*\)"\(.*\)/\1/; 1q')
        # on macOS, sed doesn't support '?'
        if [[ $ver = "1."* ]]
        then
          JAVA_VERSION=$(echo $ver | sed -e 's/1\.\([0-9]*\)\(.*\)/\1/; 1q')
        else
          JAVA_VERSION=$(echo $ver | sed -e 's/\([0-9]*\)\(.*\)/\1/; 1q')
        fi
      fi
    done

    # reset CHARSET for alibaba opts, we use GBK
    [[ -x /opt/taobao/java ]] && JVM_OPTS="-Dinput.encoding=GBK ${JVM_OPTS} "

}

# get latest version from local
get_local_version()
{
    ls "${ARTHAS_LIB_DIR}" | sort | tail -1
}

get_repo_url()
{
    local repoUrl=""
    if [[ $REPO_MIRROR == "center" ]] ; then
        repoUrl="https://repo1.maven.org/maven2"
    fi
    if [[ $REPO_MIRROR == "aliyun" ]] ; then
        repoUrl="https://maven.aliyun.com/repository/public"
    fi
    if [ -z ${repoUrl} ] ; then
        repoUrl="${REPO_MIRROR}"
    fi
    if [ "$USE_HTTP" = true ] ; then
        repoUrl=${repoUrl/https/http}
    fi
    echo "${repoUrl}"
}

# get latest version from remote
get_remote_version()
{
    local url="${MAVEN_METADATA_URL//PLACEHOLDER_REPO/$(get_repo_url)}"
    curl -sLk "${url}" | sed -n -e 's/.*<release>\(.*\)<\/release>.*/\1/p' | head -n 1
}

# check version greater
version_gt()
{
    [[ $1 == $2 ]] && return 1
    local gtVersion=`echo -e "$1\n$2" | sort | tail -1`
    [[ $gtVersion == $1 ]] && return 0 || return 1
}

# update arthas if necessary
update_if_necessary()
{
    local update_version=$1

    if [ ! -d "${ARTHAS_LIB_DIR}/${update_version}" ]; then
        echo "updating version ${update_version} ..."

        local temp_target_lib_dir="$TMP_DIR/temp_${update_version}_$$"
        local temp_target_lib_zip="${temp_target_lib_dir}/arthas-${update_version}-bin.zip"
        local target_lib_dir="${ARTHAS_LIB_DIR}/${update_version}/arthas"

        # clean
        rm -rf "${temp_target_lib_dir}"
        rm -rf "${target_lib_dir}"

        mkdir -p "${temp_target_lib_dir}" \
            || exit_on_err 1 "create ${temp_target_lib_dir} fail."

        # download current arthas version
        local downloadUrl="${REMOTE_DOWNLOAD_URL//PLACEHOLDER_REPO/$(get_repo_url)}"
        downloadUrl="${downloadUrl//PLACEHOLDER_VERSION/${update_version}}"
        echo "Download arthas from: ${downloadUrl}"
        curl \
            -#Lk \
            --connect-timeout ${SO_TIMEOUT} \
            -o "${temp_target_lib_zip}" \
            "${downloadUrl}" \
        || return 1

        # unzip arthas lib
        if ! (unzip "${temp_target_lib_zip}" -d "${temp_target_lib_dir}") ; then
            rm -rf "${temp_target_lib_dir}" "${ARTHAS_LIB_DIR}/${update_version}"
            return 1
        fi

        mkdir -p "${ARTHAS_LIB_DIR}/${update_version}"
        # rename
        mv "${temp_target_lib_dir}" "${target_lib_dir}" || return 1

        # print success
        echo "update completed."
    fi
}

call_jps()
{
    if [ "${VERBOSE}" = true ] ; then
        "${JAVA_HOME}"/bin/jps -l -v
    else
        "${JAVA_HOME}"/bin/jps -l
    fi
}

# the usage
usage()
{
    echo "
Usage:
    $0 [-h] [--target-ip <value>] [--telnet-port <value>]
       [--http-port <value>] [--session-timeout <value>] [--arthas-home <value>]
       [--use-version <value>] [--repo-mirror <value>] [--versions] [--use-http]
       [--attach-only] [-c <value>] [-f <value>] [-v] [pid]

Options and Arguments:
 -h,--help                      Print usage
    --target-ip <value>         The target jvm listen ip, default 127.0.0.1
    --telnet-port <value>       The target jvm listen telnet port, default 3658
    --http-port <value>         The target jvm listen http port, default 8563
    --session-timeout <value>   The session timeout seconds, default 300
    --arthas-home <value>       The arthas home
    --use-version <value>       Use special version arthas
    --repo-mirror <value>       Use special maven repository mirror, value is
                                center/aliyun or http repo url.
    --versions                  List local and remote arthas versions
    --use-http                  Enforce use http to download, default use https
    --attach-only               Attach target process only, do not connect
    --debug-attach              Debug attach agent
 -c,--command <value>           Command to execute, multiple commands separated
                                by ;
 -f,--batch-file <value>        The batch file to execute
 -v,--verbose                   Verbose, print debug info.
 <pid>                          Target pid

EXAMPLES:
  ./as.sh <pid>
  ./as.sh --target-ip 0.0.0.0
  ./as.sh --telnet-port 9999 --http-port -1
  ./as.sh -c 'sysprop; thread' <pid>
  ./as.sh -f batch.as <pid>
  ./as.sh --use-version 3.0.5
  ./as.sh --session-timeout 3600
  ./as.sh --attach-only
  ./as.sh --repo-mirror aliyun --use-http
WIKI:
  https://alibaba.github.io/arthas

Here is the list of possible java process(es) to attatch:
"

call_jps | grep -v sun.tools.jps.Jps

}

# list arthas versions
list_versions()
{
    echo "Arthas versions under ${ARTHAS_LIB_DIR}:"
    ls -1 "${ARTHAS_LIB_DIR}"
}

# find the process tcp listen at the port
# $1 : port number
find_listen_port_process()
{
    if [ -x "$(command -v lsof)" ]; then
        echo $(lsof -t -s TCP:LISTEN -i TCP:$1)
    fi
}

parse_arguments()
{
    POSITIONAL=()
    while [[ $# -gt 0 ]]
    do
    key="$1"

    case $key in
        -h|--help)
        usage
        exit 0
        ;;
        --versions)
        list_versions
        exit 0
        ;;
        --target-ip)
        TARGET_IP="$2"
        shift # past argument
        shift # past value
        ;;
        --telnet-port)
        TELNET_PORT="$2"
        shift # past argument
        shift # past value
        ;;
        --http-port)
        HTTP_PORT="$2"
        shift # past argument
        shift # past value
        ;;
        --session-timeout)
        SESSION_TIMEOUT="$2"
        shift # past argument
        shift # past value
        ;;
        --arthas-home)
        ARTHAS_HOME="$2"
        shift # past argument
        shift # past value
        ;;
        --use-version)
        USE_VERSION="$2"
        shift # past argument
        shift # past value
        ;;
        --repo-mirror)
        REPO_MIRROR="$2"
        shift # past argument
        shift # past value
        ;;
        -c|--command)
        COMMAND="$2"
        BATCH_MODE=true
        shift # past argument
        shift # past value
        ;;
        -f|--batch-file)
        BATCH_FILE="$2"
        BATCH_MODE=true
        shift # past argument
        shift # past value
        ;;
        --use-http)
        USE_HTTP=true
        shift # past argument
        ;;
        --attach-only)
        ATTACH_ONLY=true
        shift # past argument
        ;;
        --debug-attach)
        DEBUG_ATTACH=true
        if [ -z "$JPDA_TRANSPORT" ]; then
            JPDA_TRANSPORT="dt_socket"
        fi
        if [ -z "$JPDA_ADDRESS" ]; then
            JPDA_ADDRESS="8888"
        fi
        if [ -z "$JPDA_SUSPEND" ]; then
            JPDA_SUSPEND="y"
        fi
        if [ -z "$JPDA_OPTS" ]; then
            JPDA_OPTS="-agentlib:jdwp=transport=$JPDA_TRANSPORT,address=$JPDA_ADDRESS,server=y,suspend=$JPDA_SUSPEND"
        fi
        ARTHAS_OPTS="$JPDA_OPTS $ARTHAS_OPTS"
        shift # past argument
        ;;
        -v|--verbose)
        VERBOSE=true
        shift # past argument
        ;;
        --default)
        DEFAULT=YES
        shift # past argument
        ;;
        *)    # unknown option
        POSITIONAL+=("$1") # save it in an array for later
        shift # past argument
        ;;
    esac
    done
    set -- "${POSITIONAL[@]}" # restore positional parameters

    if [[ -n $1 ]]; then
        # parse pid
        TARGET_PID=$(echo ${1}|awk -F "@"   '{print $1}');
        local targetIp=$(echo ${1}|awk -F "@|:" '{print $2}');
        [[ "$targetIp" ]] && TARGET_IP=$targetIp
        local telnetPort=$(echo ${1}|awk -F ":"   '{print $2}');
        [[ "$telnetPort" ]] && TELNET_PORT=$telnetPort
        local httpPort=$(echo ${1}|awk -F ":"   '{print $3}');
        [[ "$httpPort" ]] && HTTP_PORT=$httpPort
    fi

    # check telnet port/http port
    local telnetPortPid
    local httpPortPid
    if [[ $TELNET_PORT > 0 ]]; then
        telnetPortPid=$(find_listen_port_process $TELNET_PORT)
        if [ $telnetPortPid ]; then
            echo "[INFO] Process $telnetPortPid already using port $TELNET_PORT"
        fi
    fi
    if [[ $HTTP_PORT > 0 ]]; then
        httpPortPid=$(find_listen_port_process $HTTP_PORT)
        if [ $telnetPortPid ]; then
            echo "[INFO] Process $httpPortPid already using port $HTTP_PORT"
        fi
    fi


    # check pid
    if [ -z ${TARGET_PID} ] && [ ${BATCH_MODE} = false ]; then
        # interactive mode
        local IFS=$'\n'
        CANDIDATES=($(call_jps | grep -v sun.tools.jps.Jps | awk '{print $0}'))

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

        # check the process already using telnet port if equals to target pid
        if [[ ($telnetPortPid) && ($TARGET_PID != $telnetPortPid) ]]; then
            echo "[ERROR] Target process $TARGET_PID is not the process using port $TELNET_PORT, you will connect to an unexpected process."
            echo "[ERROR] If you still want to attach target process $TARGET_PID, Try to set a different telnet port by using --telnet-port argument."
            echo "[ERROR] Or try to shutdown the process $telnetPortPid using the telnet port first."
            exit 1
        fi
        if [[ ($httpPortPid) && ($TARGET_PID != $httpPortPid) ]]; then
            echo "Target process $TARGET_PID is not the process using port $HTTP_PORT, you will connect to an unexpected process."
            echo "If you still want to attach target process $TARGET_PID, Try to set a different telnet port by using --telnet-port argument."
            echo "Or try to shutdown the process $httpPortPid using the telnet port first."
            exit 1
        fi
    elif [ -z ${TARGET_PID} ]; then
        # batch mode is enabled, no interactive process selection.
        echo "Illegal arguments, the <PID> is required." 1>&2
        return 1
    fi
}

# attach arthas to target jvm
attach_jvm()
{
    local arthas_lib_dir=$1
    # http://www.inonit.com/cygwin/faq/
    if [ "${OS_TYPE}" = "Cygwin" ]; then
        arthas_lib_dir=`cygpath -wp "$arthas_lib_dir"`
    fi

    echo "Attaching to ${TARGET_PID} using version ${1}..."

    local java_command=("${JAVA_HOME}"/bin/java)
    if [ "${BOOT_CLASSPATH}" ]; then
        java_command+=("${BOOT_CLASSPATH}")
    fi

    "${java_command[@]}" \
        ${ARTHAS_OPTS} ${JVM_OPTS} \
        -jar "${arthas_lib_dir}/arthas-core.jar" \
            -pid ${TARGET_PID} \
            -target-ip ${TARGET_IP} \
            -telnet-port ${TELNET_PORT} \
            -http-port ${HTTP_PORT} \
            -session-timeout ${SESSION_TIMEOUT} \
            -core "${arthas_lib_dir}/arthas-core.jar" \
            -agent "${arthas_lib_dir}/arthas-agent.jar"

}

sanity_check() {
    # only Linux/Mac support ps to find process, Cygwin/MinGw may fail.
    if ([ "${OS_TYPE}" != "Linux" ] && [ "${OS_TYPE}" != "Mac" ]); then
        return
    fi
 
    # 0 check whether the pid exist
    local pid=$(ps -p ${TARGET_PID} -o pid= 2>&1 )

    # get ps command exit code
    local exitCode="$(ps -p ${TARGET_PID} -o pid= > /dev/null 2>&1; echo $?)"

    # If ps exist code not 0, the TARGET_PID process maybe not exist or ps do not support -p options.
    if [ "${exitCode}" != "0" ]; then
        # if ps do not support -p or -o , ${pid} will be error message, just return
        if [ -n "${pid}" ]; then
            return
        fi
    fi

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
# $1 : arthas_lib_dir
active_console()
{
    local arthas_lib_dir=$1

    # http://www.inonit.com/cygwin/faq/
    if [ "${OS_TYPE}" = "Cygwin" ]; then
        arthas_lib_dir=`cygpath -wp $arthas_lib_dir`
    fi

    if [ "${BATCH_MODE}" = "true" ]; then
        if [ "${COMMAND}" ] ; then
        "${JAVA_HOME}/bin/java" ${ARTHAS_OPTS} ${JVM_OPTS} \
             -jar "${arthas_lib_dir}/arthas-client.jar" \
             ${TARGET_IP} \
             ${TELNET_PORT} \
             -c ${COMMAND}
        fi
        if [ "${BATCH_FILE}" ] ; then
        "${JAVA_HOME}/bin/java" ${ARTHAS_OPTS} ${JVM_OPTS} \
             -jar "${arthas_lib_dir}/arthas-client.jar" \
             ${TARGET_IP} \
             ${TELNET_PORT} \
             -f ${BATCH_FILE}
        fi
    elif type telnet 2>&1 >> /dev/null; then
        # use telnet
        if [[ $(command -v telnet) == *"system32"* ]] ; then
            # Windows/system32/telnet.exe can not run in Cygwin/MinGw
            echo "It seems that current bash is under Windows. $(command -v telnet) can not run under bash."
            echo "Please start cmd.exe from Windows start menu, and then run telnet ${TARGET_IP} ${TELNET_PORT} to connect to target process."
            echo "Or visit http://127.0.0.1:${HTTP_PORT} to connect to target process."
            return 1
        fi
        echo "telnet connecting to arthas server... current timestamp is `date +%s`"
        telnet ${TARGET_IP} ${TELNET_PORT}
    else
        echo "'telnet' is required." 1>&2
        return 1
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

    # try to find arthas home from --use-version
    if [[ (-z "${ARTHAS_HOME}")  && (! -z "${USE_VERSION}") ]]; then
        if [[ ! -d "${ARTHAS_LIB_DIR}/${USE_VERSION}/arthas" ]] ; then
            update_if_necessary "${USE_VERSION}" || echo "update fail, ignore this update." 1>&2
        fi
        ARTHAS_HOME="${ARTHAS_LIB_DIR}/${USE_VERSION}/arthas"
    fi

    # try to set arthas home from as.sh directory
    if [ -z "${ARTHAS_HOME}" ] ; then
        [[ -a "${DIR}/arthas-core.jar" ]] \
        && [[ -a "${DIR}/arthas-agent.jar" ]] \
        && [[ -a "${DIR}/arthas-spy.jar" ]] \
        && ARTHAS_HOME="${DIR}"
    fi

    # try to find arthas under ~/.arthas/lib
    if [ -z "${ARTHAS_HOME}" ] ; then
        local remote_version=$(get_remote_version)
        local arthas_local_version=$(get_local_version)
        if $(version_gt $remote_version $arthas_local_version) ; then
            update_if_necessary "${remote_version}" || echo "update fail, ignore this update." 1>&2
        fi
        local arthas_local_version=$(get_local_version)
        ARTHAS_HOME="${ARTHAS_LIB_DIR}/${arthas_local_version}/arthas"
    fi

    echo "Arthas home: ${ARTHAS_HOME}"

    if [ ! -d "${ARTHAS_HOME}" ] ; then
        exit_on_err 1 "Arthas home is not a directory, please delete it and retry."
    fi

    sanity_check

    echo "Calculating attach execution time..."
    time (attach_jvm "${ARTHAS_HOME}" || exit 1)

    if [ $? -ne 0 ]; then
        exit_on_err 1 "attach to target jvm (${TARGET_PID}) failed, check ${HOME}/logs/arthas/arthas.log or stderr of target jvm for any exceptions."
    fi

    echo "Attach success."

    if [ ${ATTACH_ONLY} = false ]; then
      active_console "${ARTHAS_HOME}"
    fi
}



main "${@}"
