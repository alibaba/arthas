#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

get_local_maven_project_version()
{
    "$DIR/mvnw" -T 2C -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate \
     -Dexpression=project.version -f $DIR/pom.xml -B | grep -e '^[^\[]' | cut -b 1-5
}

"$DIR/mvnw" -version

CUR_VERSION=$(get_local_maven_project_version)

# arthas's version
DATE=$(date '+%Y%m%d%H%M%S')

ARTHAS_VERSION="${CUR_VERSION}.${DATE}"

echo "${ARTHAS_VERSION}" > $DIR/core/src/main/resources/com/taobao/arthas/core/res/version

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
"$DIR/mvnw" clean package -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -f $DIR/pom.xml \
|| exit_on_err 1 "package arthas failed."

rm -r "$DIR/core/src/main/resources/com/taobao/arthas/core/res/version"

packaging_bin_path=$(ls "${DIR}"/packaging/target/arthas-bin.zip)

# install to local
mkdir -p "${NEWEST_ARTHAS_LIB_HOME}"
unzip ${packaging_bin_path} -d "${NEWEST_ARTHAS_LIB_HOME}/"

# print ~/.arthas directory size
arthas_dir_size="$(du -hs ${HOME}/.arthas | cut -f1)"
echo "${HOME}/.arthas size: ${arthas_dir_size}"