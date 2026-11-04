#!/bin/bash

# define newest arthas's version
ARTHAS_VERSION=${project.version}

# define newest arthas's lib home
ARTHAS_LIB_HOME=${HOME}/.arthas/lib/${ARTHAS_VERSION}/arthas

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}

# install to local if necessary
if [[ ! -x ${ARTHAS_LIB_HOME} ]]; then

    # install to local
    mkdir -p ${ARTHAS_LIB_HOME} \
    || exit_on_err 1 "create target directory ${ARTHAS_LIB_HOME} failed."

    # copy jar files
    cp *.jar ${ARTHAS_LIB_HOME}/

    # make it -x
    chmod +x ./as.sh

fi

echo "install to local succeeded."

