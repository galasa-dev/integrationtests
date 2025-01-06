#! /usr/bin/env bash 

#
# Copyright contributors to the Galasa project
#
# SPDX-License-Identifier: EPL-2.0
#
#-----------------------------------------------------------------------------------------                   
#
# Objectives: Build this repository code locally.
#
# Environment variable over-rides:
# LOGS_DIR - Optional. Where logs are placed. Defaults to creating a temporary directory.
# SOURCE_MAVEN - Optional. Where a maven repository is from which the build will draw artifacts.
# DEBUG - Optional. Defaults to 0 (off)
# 
#-----------------------------------------------------------------------------------------                   

# Where is this script executing from ?
BASEDIR=$(dirname "$0");pushd $BASEDIR 2>&1 >> /dev/null ;BASEDIR=$(pwd);popd 2>&1 >> /dev/null
echo "Running from directory ${BASEDIR}"
export ORIGINAL_DIR=$(pwd)
cd "${BASEDIR}"


#-----------------------------------------------------------------------------------------                   
#
# Set Colors
#
#-----------------------------------------------------------------------------------------                   
bold=$(tput bold)
underline=$(tput sgr 0 1)
reset=$(tput sgr0)
red=$(tput setaf 1)
green=$(tput setaf 76)
white=$(tput setaf 7)
tan=$(tput setaf 202)
blue=$(tput setaf 25)

#-----------------------------------------------------------------------------------------                   
#
# Headers and Logging
#
#-----------------------------------------------------------------------------------------                   
underline() { printf "${underline}${bold}%s${reset}\n" "$@"
}
h1() { printf "\n${underline}${bold}${blue}%s${reset}\n" "$@"
}
h2() { printf "\n${underline}${bold}${white}%s${reset}\n" "$@"
}
debug() { printf "${white}%s${reset}\n" "$@"
}
info() { printf "${white}➜ %s${reset}\n" "$@"
}
success() { printf "${green}✔ %s${reset}\n" "$@"
}
error() { printf "${red}✖ %s${reset}\n" "$@"
}
warn() { printf "${tan}➜ %s${reset}\n" "$@"
}
bold() { printf "${bold}%s${reset}\n" "$@"
}
note() { printf "\n${underline}${bold}${blue}Note:${reset} ${blue}%s${reset}\n" "$@"
}

#-----------------------------------------------------------------------------------------                   
# Main logic.
#-----------------------------------------------------------------------------------------                   
project="inttests"
source_dir="galasa-inttests-parent"
h1 "Building ${project}"

# Debug or not debug ? Override using the DEBUG flag.
if [[ -z ${DEBUG} ]]; then
    export DEBUG=0
    # export DEBUG=1
    info "DEBUG defaulting to ${DEBUG}."
    info "Over-ride this variable if you wish. Valid values are 0 and 1."
else
    info "DEBUG set to ${DEBUG} by caller."
fi

# Over-rode SOURCE_MAVEN if you want to build from a different maven repo...
if [[ -z ${SOURCE_MAVEN} ]]; then
    export SOURCE_MAVEN=https://development.galasa.dev/main/maven-repo/managers
    info "SOURCE_MAVEN repo defaulting to ${SOURCE_MAVEN}."
    info "Set this environment variable if you want to over-ride this value."
else
    info "SOURCE_MAVEN set to ${SOURCE_MAVEN} by caller."
fi

# Create a temporary dir.
# Note: This bash 'spell' works in OSX and Linux.
if [[ -z ${LOGS_DIR} ]]; then
    export LOGS_DIR=$(mktemp -d 2>/dev/null || mktemp -d -t "galasa-logs")
    info "Logs are stored in the ${LOGS_DIR} folder."
    info "Over-ride this setting using the LOGS_DIR environment variable."
else
    mkdir -p ${LOGS_DIR} 2>&1 > /dev/null # Don't show output. We don't care if it already existed.
    info "Logs are stored in the ${LOGS_DIR} folder."
    info "Over-ridden by caller using the LOGS_DIR variable."
fi

if [[ "${DEBUG}" == "1" ]]; then
    OPTIONAL_DEBUG_FLAG="-debug"
else
    OPTIONAL_DEBUG_FLAG="-info"
fi

# auto plain rich or verbose
CONSOLE_FLAG=--console=plain

log_file=${LOGS_DIR}/${project}.txt
info "Log will be placed at ${log_file}"


function gradle_build {
    h2 "Building using Gradle..."

    info "Using source code at ${source_dir}"
    cd ${BASEDIR}/${source_dir}

    cmd="gradle --build-cache --parallel \
    ${CONSOLE_FLAG} \
    --warning-mode=all \
    -Dorg.gradle.java.home=${JAVA_HOME} \
    -PsourceMaven=https://development.galasa.dev/main/maven-repo/obr ${OPTIONAL_DEBUG_FLAG} \
    -PcentralMaven=https://repo.maven.apache.org/maven2/ \
    build publishToMavenLocal \
    "

    info "Using this command: $cmd"
    $cmd 2>&1 >> ${log_file}
    rc=$? ; if [[ "${rc}" != "0" ]]; then error "Failed to build ${project} log is at ${log_file}" ; exit 1 ; fi
    success "Built OK"
}

function build_obr {
    h2 "Building OBR using Maven..."
    cd ${BASEDIR}/galasa-inttests-parent/dev.galasa.inttests.obr

    context=inttests/galasa-inttests-parent/dev.galasa.inttests.obr
    bootstrap=https://galasa-ecosystem1.galasa.dev/api/bootstrap

    cmd="mvn \
    -Dgalasa.source.repo=https://development.galasa.dev/main/maven-repo/obr \
    -Dgalasa.central.repo=https://repo.maven.apache.org/maven2/ \
    install
    "

    info "Using this command: $cmd"
    $cmd 2>&1 >> ${log_file}
    rc=$? ; if [[ "${rc}" != "0" ]]; then error "Failed to build ${project} log is at ${log_file}" ; exit 1 ; fi
    success "Built OBR OK"
}

mkdir -p $BASEDIR/temp
gradle_build
build_obr