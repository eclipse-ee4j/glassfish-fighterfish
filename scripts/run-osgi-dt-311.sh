#!/bin/sh -x
#
# Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v. 2.0, which is available at
# http://www.eclipse.org/legal/epl-2.0.
#
# This Source Code may also be made available under the following Secondary
# Licenses when the conditions for such availability set forth in the
# Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
# version 2 with the GNU Classpath Exception, which is available at
# https://www.gnu.org/software/classpath/license.html.
#
# SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
#

if [ $# -eq 0 ]
then
 echo "Usage: $0 <glassfish.home>"
 exit 1
fi
GF_HOME=$1
shift 1
WS=~/WS/ff/fighterfish-gf3.1.1/test/test.it
mkdir ${GF_HOME}/osgi/felix/conf/
cp ${GF_HOME}/config/osgi.properties ${GF_HOME}/osgi/felix/conf/config.properties
mkdir ${GF_HOME}/osgi/equinox/configuration/
cp ${GF_HOME}/config/osgi.properties ${GF_HOME}/osgi/equinox/configuration/config.ini
OUT=${WS}/t.log
touch ${OUT}
if [ -w ${OUT} ]
then
 rm ${OUT}
 #echo "Running tests on Equinox"
 #redirect.sh ${OUT} `which mvn` -o -f ${WS}/pom.xml clean test -P-Felix -PEquinox -Dglassfish.home=${GF_HOME} $*
 #rm -rf ${WS}/surefire-reports-Equinox || true
 #mv ${WS}/target/surefire-reports ${WS}/surefire-reports-Equinox
 #rm ${GF_HOME}/osgi/equinox/configuration/config.ini
 echo "Running tests on Felix"
 redirect.sh ${OUT} `which mvn` -o -f ${WS}/pom.xml clean test -Dglassfish.home=${GF_HOME} $*
 rm -rf ${WS}/surefire-reports-Felix || true
 mv ${WS}/target/surefire-reports ${WS}/surefire-reports-Felix
 rm ${GF_HOME}/osgi/felix/conf/config.properties
 echo "Summary:" 
 grep "Tests run" ${OUT}
else
 echo "can't write t.log."
 exit 1
fi
