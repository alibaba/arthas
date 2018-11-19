#!/usr/bin/env bash

# WIKI: https://alibaba.github.io/arthas
# This script only supports bash, do not support posix sh.
# If you have the problem like Syntax error: "(" unexpected (expecting "fi"),
# Try to run "bash -version" to check the version.
# Try to visit WIKI to find a solution.

# program : Arthas
#  author : Core Engine @ Taobao.com
#    date : 2018-11-19

# current arthas script version
ARTHAS_SCRIPT_VERSION=3.0.4.2

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
  echo 'Error: curl is not installed.' >&2
  exit 1
fi
if ! [ -x "$(command -v grep)" ]; then
  echo 'Error: grep is not installed.' >&2
  exit 1
fi
if ! [ -x "$(command -v awk)" ]; then
  echo 'Error: awk is not installed.' >&2
  exit 1
fi
if ! [ -x "$(command -v telnet)" ]; then
  echo 'Error: telnet is not installed.' >&2
  exit 1
fi
if ! [ -x "$(command -v unzip)" ]; then
  echo 'Error: unzip is not installed.' >&2
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

    echo "JAVA_HOME: ${JAVA_HOME}"

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

    # when java version greater than 9, there is no tools.jar
    if [[ "$JAVA_VERSION" -lt 9 ]];then
      # check tools.jar exists
      if [ ! -f "${JAVA_HOME}/lib/tools.jar" ]; then
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
    ls "${ARTHAS_LIB_DIR}" | sort | tail -1
}

# get latest version from remote
get_remote_version()
{
    curl -sLk --connect-timeout ${SO_TIMEOUT} "${ARTHAS_REMOTE_VERSION_URL}" | sed 's/{.*latestVersion":"*\([0-9a-zA-Z\\.\\-]*\)"*,*.*}/\1/'
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
        mkdir -p "${target_lib_dir}"

        # clean
        rm -rf "${temp_target_lib_dir}"
        rm -rf "${target_lib_dir}"

        mkdir -p "${temp_target_lib_dir}" \
            || exit_on_err 1 "create ${temp_target_lib_dir} fail."

        # download current arthas version
        curl \
            -#Lk \
            --connect-timeout ${SO_TIMEOUT} \
            -o "${temp_target_lib_zip}" \
            "${ARTHAS_REMOTE_DOWNLOAD_URL}/${update_version}/arthas-packaging-${update_version}-bin.zip"  \
        || return 1

        # unzip arthas lib
        unzip "${temp_target_lib_zip}" -d "${temp_target_lib_dir}" || (rm -rf "${temp_target_lib_dir}" \
        "${ARTHAS_LIB_DIR}/${update_version}" && return 1)

        # rename
        mv "${temp_target_lib_dir}" "${target_lib_dir}" || return 1

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
    [--versions]    : list all arthas versions.

Example:
    ./as.sh <PID>
    ./as.sh <PID>@[IP]
    ./as.sh <PID>@[IP:PORT]
    ./as.sh debug <PID>
    ./as.sh -b <PID>
    ./as.sh -b -f /path/to/script
    ./as.sh --attach-only <PID>
    ./as.sh --use-version 3.0.5.20180919185025 <PID>
    ./as.sh --versions

Here is the list of possible java process(es) to attatch:
"

"${JAVA_HOME}"/bin/jps -l | grep -v sun.tools.jps.Jps

}

# list arthas versions
list_versions()
{
    echo "Arthas versions under ${ARTHAS_LIB_DIR}:"
    ls -1 "${ARTHAS_LIB_DIR}"
}

# parse the argument
parse_arguments()
{
    if ([ "$1" = "-h" ] || [ "$1" = "--help" ] || [ "$1" = "-help" ]) ; then
        usage
        exit 0
    fi

    if ([ "$1" = "--versions" ]) ; then
        list_versions
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
        JPDA_SUSPEND="y"
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
        # backup IFS: https://github.com/alibaba/arthas/issues/128
        local IFS_backup=$IFS
        IFS=$'\n'
        CANDIDATES=($("${JAVA_HOME}"/bin/jps -l | grep -v sun.tools.jps.Jps | awk '{print $0}'))

        if [ ${#CANDIDATES[@]} -eq 0 ]; then
            echo "Error: no available java process to attach."
            # recover IFS
            IFS=$IFS_backup
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
        # recover IFS
        IFS=$IFS_backup
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

    # http://www.inonit.com/cygwin/faq/
    if [ "${OS_TYPE}" = "Cygwin" ]; then
        arthas_lib_dir=`cygpath -wp $arthas_lib_dir`
    fi

    echo "Attaching to ${TARGET_PID} using version ${1}..."

    local opts="${ARTHAS_OPTS} ${BOOT_CLASSPATH} ${JVM_OPTS}"
    if [ ${TARGET_IP} = ${DEFAULT_TARGET_IP} ]; then
        "${JAVA_HOME}"/bin/java \
            ${opts}  \
            -jar "${arthas_lib_dir}/arthas-core.jar" \
                -pid ${TARGET_PID} \
                -target-ip ${TARGET_IP} \
                -telnet-port ${TELNET_PORT} \
                -http-port ${HTTP_PORT} \
                -core "${arthas_lib_dir}/arthas-core.jar" \
                -agent "${arthas_lib_dir}/arthas-agent.jar"
    fi
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
# $1 : arthas_local_version
active_console()
{
    local arthas_version=$1
    local arthas_lib_dir=${ARTHAS_LIB_DIR}/${arthas_version}/arthas

    # http://www.inonit.com/cygwin/faq/
    if [ "${OS_TYPE}" = "Cygwin" ]; then
        arthas_lib_dir=`cygpath -wp $arthas_lib_dir`
    fi

    if [ "${BATCH_MODE}" = "true" ]; then
        "${JAVA_HOME}/bin/java" ${ARTHAS_OPTS} ${JVM_OPTS} \
             -jar "${arthas_lib_dir}/arthas-client.jar" \
             ${TARGET_IP} \
             -p ${TELNET_PORT} \
             -f ${BATCH_SCRIPT}
    elif type telnet 2>&1 >> /dev/null; then
        # use telnet
        if [[ $(command -v telnet) == *"system32"* ]] ; then
            # Windows/system32/telnet.exe can not run in Cygwin/MinGw
            echo "It seems that current bash is under Windows. $(command -v telnet) can not run under bash."
            echo "Please start cmd.exe from Windows start menu, and then run telnet ${TARGET_IP} ${TELNET_PORT} to connect to target process."
            echo "Or visit http://127.0.0.1:8563/ to connect to target process."
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

    local remote_version=$(get_remote_version)

    if [ -z "${ARTHAS_VERSION}" ]; then
        update_if_necessary "${remote_version}" || echo "update fail, ignore this update." 1>&2
    else
        update_if_necessary "${ARTHAS_VERSION}" || echo "update fail, ignore this update." 1>&2
    fi

    local arthas_local_version=$(get_local_version)

    if [ ! -z "${ARTHAS_VERSION}" ]; then
        arthas_local_version=${ARTHAS_VERSION}
    fi

    if [ ! -d "${ARTHAS_LIB_DIR}/${arthas_local_version}" ]; then
        exit_on_err 1 "arthas not found, please check your network."
    fi

    sanity_check

    echo "Calculating attach execution time..."
    time (attach_jvm "${arthas_local_version}" || exit 1)

    if [ $? -ne 0 ]; then
        exit_on_err 1 "attach to target jvm (${TARGET_PID}) failed, check ${HOME}/logs/arthas/arthas.log or stderr of target jvm for any exceptions."
    fi

    echo "Attach success."

    if [ ${ATTACH_ONLY} = false ]; then
      active_console ${arthas_local_version}
    fi
}



main "${@}"
