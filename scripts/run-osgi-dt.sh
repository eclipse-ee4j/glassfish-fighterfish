#!/bin/bash +x
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

set +e
if [ $# -eq 0 ] 
then
 echo "Usage: $0 <glassfish.home>"
 exit 1
fi
GF_HOME=$1
shift 1
# Some constants
# Where is the workspace?
WS=~/WS/ff/trunk/test/it
OUT=${WS}/t.log
# Total no. of tests in the test suite
EXPECTED_RESULT=`cat $WS/ExpectedTestResult.txt`

# Arguments: ResultVariableName
function equinox() {
 echo "Running tests on Equinox"
 local var_name=$1
 shift 1
 cat ${OUT} >> ${OUT}.old
 rm ${OUT}
 redirect.sh ${OUT} ~/software/apache-maven-2.2.1/bin/mvn -o -f ${WS}/pom.xml clean test -P-Felix -PEquinox -Dglassfish.home=${GF_HOME} $*
 rm -rf ${WS}/surefire-reports-Equinox || true
 mv ${WS}/target/surefire-reports ${WS}/surefire-reports-Equinox
 #Make sure we have run all the tests.
 testReport ${OUT} ${var_name}
 return $?
}

# Arguments: ResultVariableName
function felix() {
 echo "Running tests on Felix"
 local var_name=$1
 shift 1
 cat ${OUT} >> ${OUT}.old
 rm ${OUT}
 redirect.sh ${OUT}  ~/software/apache-maven-2.2.1/bin/mvn -o -f ${WS}/pom.xml clean test -Dglassfish.home=${GF_HOME} $*
 rm -rf ${WS}/surefire-reports-Felix || true
 mv ${WS}/target/surefire-reports ${WS}/surefire-reports-Felix
 # Make sure we have run all the tests. Look at the last line as multiple lines match the pattern
 testReport ${OUT} ${var_name}
 return $?
}

# Arguments: ${OUT} ResultVariableName
function testReport() {
 local  __resultvar=$2
 local report=`grep "Tests run" $1`
 local return_val=0
 ACTUAL_RESULT=`grep "Tests run" $1 | tail -1`
 if [ "$ACTUAL_RESULT" != "$EXPECTED_RESULT" ]
 then
  mismatch="FAILED: Expected and actual results differ: Actual=$ACTUAL_RESULT vs. Expected=$EXPECTED_RESULT"
  report="$report 
$mismatch"
  return_val=1
 fi
 eval $__resultvar="'$report'"
 return $return_val
}

touch ${OUT}
if [ -w ${OUT} ]
then
 felix FelixResult $*
 Felix=$?
 echo Skipping equinox for now due to GLASSFISH-18975
# equinox EquinoxResult $*
 Equinox=$?
 echo "*************SUMARRY***************"
 echo ""
 echo "Felix Test Result:"
 echo "$FelixResult"
 echo ""
 echo "Equinox Test Result:"
 echo "$EquinoxResult"
 echo ""
 exit `expr $Felix + $Equinox`
else
 echo "can't write t.log."
 exit 1
fi
