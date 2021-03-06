#!/bin/bash

# Copyright (C) 2020 Timothy "ZeevoX" Langer
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Output Colours

RED=$'\e[0;31m'
GREEN=$'\e[1;32m'
BLUE=$'\e[0;33m'
YELLOW=$'\e[1;33m'
WHITE=$'\e[0m'

# Install Linters if not already installed
# Hides error messages if not installed

echo ''

if beautysh > /dev/null 2>&1; then
  echo $YELLOW BeautySh already installed $WHITE
else
  echo $YELLOW Installing BeautySh $WHITE
  if pip3 install beautysh; then
    echo $GREEN BeautySh Successfully Installed $WHITE
    echo ''
  else
    echo $RED Failed to install BeautySh $WHITE
    echo ''
  fi
fi

if align > /dev/null 2>&1; then
  echo $YELLOW Align-YAML already installed $WHITE
else
  echo $YELLOW Installing Align-YAML $WHITE
  if npm i -g align-yaml; then
    echo $GREEN Align-YAML Successfully Installed
    echo ''
  else
    echo $RED Failed to install Align-YAML $WHITE
    echo ''
  fi
fi

echo ''

# Used to count how many successful/unsuccesful lints there were

SUCCESS=0
UNSUCCESS=0

# Download google-java-format to lint Java files
curl -s https://api.github.com/repos/google/google-java-format/releases/latest \
| grep "browser_download_url.*-all-deps.jar" \
| cut -d : -f 2,3 \
| tr -d \" \
| wget -qi -

cd .. # Changes to main Ocquarium directory

echo $BLUE Linting Java Files $WHITE
if java -jar scripts/google-java-format-*-all-deps.jar -i $(find . -type f -name "*.java"); then
  echo $GREEN Java Files Linted $WHITE
  (( SUCCESS=SUCCESS+1 ))
else
  echo $RED Java FIles Could Not Be Linted $WHITE
  (( UNSUCCESS=UNSUCCESS+1 ))
fi

echo $BLUE Linting YAML Files $WHITE
if align $(find . -type f -name "*.yml") > /dev/null; then
  echo $GREEN YAML Files Linted $WHITE
  (( SUCCESS=SUCCESS+1 ))
else
  echo $RED YAML Files Could Not Be Linted $WHITE
  (( UNSUCCESS=UNSUCCESS+1 ))
fi

cd scripts || exit # Shell files only in scripts folder are linted
echo $BLUE Linting Shell Files $WHITE
if beautysh -f ./*.sh > /dev/null; then
  echo $GREEN Shell Files Linted $WHITE
  (( SUCCESS=SUCCESS+1 ))
else
  echo $RED Shell Files Could Not Be Linted $WHITE
  (( UNSUCCESS=UNSUCCESS+1 ))
fi

# Outputs success of linting

echo ''
if [ "${SUCCESS}" -gt 0 ] && [ "${UNSUCCESS}" != 0 ]; then
  echo $GREEN $SUCCESS Successful Lints $WHITE
  echo $RED $UNSUCCESS Unsucessful Lints $WHITE
fi
if [ "${SUCCESS}" == 0 ]; then
  echo $RED No Successful Lints $WHITE
fi
if [ "${UNSUCCESS}" == 0 ]; then
  echo $GREEN All Lints Successful $WHITE
fi
echo ''
