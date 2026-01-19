#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

usage() {
    cat <<EOF
Usage: $(basename "$0") [options] [-- <extra mvn args>]

Options:
  --fast              本地快速打包：不执行 clean + 跳过 site 前端构建 + Maven 并行构建(-T 1C)
  --no-clean          不执行 Maven clean（保留各模块 target 缓存）
  --skip-site         跳过 site 模块的前端构建（vuepress/yarn），不影响默认打包产物
  -T, --threads <arg> 传给 Maven 的并行线程数（如 1C/4）
  -h, --help          显示帮助
EOF
}

NO_CLEAN=false
SKIP_SITE=false
MVN_THREADS=""
MVN_EXTRA_ARGS=()

while [[ $# -gt 0 ]]; do
    case "$1" in
        --fast)
            NO_CLEAN=true
            SKIP_SITE=true
            [[ -z "${MVN_THREADS}" ]] && MVN_THREADS="1C"
            shift
            ;;
        --no-clean)
            NO_CLEAN=true
            shift
            ;;
        --skip-site)
            SKIP_SITE=true
            shift
            ;;
        -T|--threads)
            if [[ $# -lt 2 ]]; then
                echo "Missing value for $1" 1>&2
                usage 1>&2
                exit 2
            fi
            MVN_THREADS="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        --)
            shift
            MVN_EXTRA_ARGS+=("$@")
            break
            ;;
        *)
            MVN_EXTRA_ARGS+=("$1")
            shift
            ;;
    esac
done

get_local_maven_project_version()
{
    "$DIR/mvnw" org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate \
     -Dexpression=project.version -q -DforceStdout -f "$DIR/pom.xml"
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
MVN_ARGS=(-f "$DIR/pom.xml" -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true)
if [[ -n "${MVN_THREADS}" ]]; then
    MVN_ARGS+=(-T "${MVN_THREADS}")
fi
if [[ "${SKIP_SITE}" == "true" ]]; then
    MVN_ARGS+=(-Darthas.site.frontend.skip=true)
fi
if [[ "${NO_CLEAN}" == "true" ]]; then
    MVN_GOALS=(package)
else
    MVN_GOALS=(clean package)
fi

# maven package the arthas
"$DIR/mvnw" "${MVN_ARGS[@]}" "${MVN_EXTRA_ARGS[@]}" "${MVN_GOALS[@]}" \
|| exit_on_err 1 "package arthas failed."

rm -r "$DIR/core/src/main/resources/com/taobao/arthas/core/res/version"

packaging_bin_path=$(ls "${DIR}"/packaging/target/arthas-bin.zip)

# install to local
mkdir -p "${NEWEST_ARTHAS_LIB_HOME}"
unzip ${packaging_bin_path} -d "${NEWEST_ARTHAS_LIB_HOME}/"

# print ~/.arthas directory size
arthas_dir_size="$(du -hs ${HOME}/.arthas | cut -f1)"
echo "${HOME}/.arthas size: ${arthas_dir_size}"
