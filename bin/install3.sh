#! /bin/bash

# temp file of as3.sh
TEMP_ARTHAS_FILE="./as3.sh.$$"

# target file of as3.sh
TARGET_ARTHAS_FILE="./as3.sh"

# update timeout(sec)
SO_TIMEOUT=60

# default downloading url
ARTHAS_FILE_URL="https://arthas.aliyun.com/as3.sh"

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}

# check permission to download && install
[[ ! -w ./ ]] && exit_on_err 1 "permission denied, target directory ./ was not writable."

if [[ $# -gt 1 ]] && [[ $1 = "--url" ]]; then
  shift
  ARTHAS_FILE_URL=$1
  shift
fi

# download from aliyunos
echo "downloading... ${TEMP_ARTHAS_FILE}"
curl \
    -sLk \
    --connect-timeout ${SO_TIMEOUT} \
    ${ARTHAS_FILE_URL} \
    -o ${TEMP_ARTHAS_FILE} \
|| exit_on_err 1 "download failed!"

# write or overwrite local file
rm -rf as3.sh
mv ${TEMP_ARTHAS_FILE} ${TARGET_ARTHAS_FILE}
chmod +x ${TARGET_ARTHAS_FILE}

# done
echo "Arthas install succeeded."
