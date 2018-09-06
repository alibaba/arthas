#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

CUR_VERSION="3.0.3"

# arthas's target dir
ARTHAS_TARGET_DIR=$DIR/target/arthas

# arthas's version
DATE=$(date '+%Y%m%d%H%M%S')

ARTHAS_VERSION="${CUR_VERSION}.${DATE}"

echo ${ARTHAS_VERSION} > $DIR/core/src/main/resources/com/taobao/arthas/core/res/version

# define newset arthas lib home
NEWEST_ARTHAS_LIB_HOME=${HOME}/.arthas/lib/${ARTHAS_VERSION}/arthas


# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}

# maven package the arthas
mvn clean package -Dmaven.test.skip=true -f $DIR/pom.xml \
|| exit_on_err 1 "package arthas failed."

rm -r $DIR/core/src/main/resources/com/taobao/arthas/core/res/version

# reset the target dir
mkdir -p ${ARTHAS_TARGET_DIR}

# copy jar to TARGET_DIR
cp $DIR/spy/target/arthas-spy.jar ${ARTHAS_TARGET_DIR}/arthas-spy.jar
cp $DIR/core/target/arthas-core-jar-with-dependencies.jar ${ARTHAS_TARGET_DIR}/arthas-core.jar
cp $DIR/agent/target/arthas-agent-jar-with-dependencies.jar ${ARTHAS_TARGET_DIR}/arthas-agent.jar
cp $DIR/client/target/arthas-client-jar-with-dependencies.jar ${ARTHAS_TARGET_DIR}/arthas-client.jar

# copy shell to TARGET_DIR
cat $DIR/bin/install-local.sh|sed "s/ARTHAS_VERSION=0.0/ARTHAS_VERSION=${ARTHAS_VERSION}/g" > ${ARTHAS_TARGET_DIR}/install-local.sh
chmod +x ${ARTHAS_TARGET_DIR}/install-local.sh
cp $DIR/bin/as.sh ${ARTHAS_TARGET_DIR}/as.sh
cp $DIR/bin/as.bat ${ARTHAS_TARGET_DIR}/as.bat

# zip the arthas
cd $DIR/target/
zip -r arthas-${ARTHAS_VERSION}-bin.zip arthas/
cd -

# install to local
mkdir -p ${NEWEST_ARTHAS_LIB_HOME}
cp $DIR/target/arthas/* ${NEWEST_ARTHAS_LIB_HOME}/

if [ $# -gt 0 ] && [ "$1" = "-release" ]; then
  echo "creating git tag ${ARTHAS_VERSION}..."
  git tag -a ${ARTHAS_VERSION} -m "release ${ARTHAS_VERSION}"
  if [ $? -eq 0 ]; then
    echo "A local git tag ${ARTHAS_VERSION} has been created, please use 'git tag -l' to verify it."
    echo "To commit the tag to remote repo, please run 'git push --tags' manually. "
  fi
fi
