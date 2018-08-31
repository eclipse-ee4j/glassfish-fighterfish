#!/bin/sh +x
#
# Copyright (c) 2013, 2018 Oracle and/or its affiliates. All rights reserved.
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

# A simple script that parses report file produced by bnd and produces a summary of
# total no. of failed tests and total no. of tests.
# It returns zero if there are no failures, else it returns an exist status which is same 
# as no. of failed tests. If no tests have run, then it returns -1.
# Author: sanjeeb.sahoo@oracle.com

EXPECTED_ARGS=1
if [ $# -ne $EXPECTED_ARGS ]
then
 echo "Usage: $0 <path to report file. e.g., reports/bnd.xml>"
 exit 1
fi
result=`grep -e failure -e error $1 | cut -d "=" -f2 | cut -d "'" -f2 | cut -d "(" -f1 | grep test `
total=`grep "test name" $1 | wc -l`
failed=`echo $result | wc -w`
echo $result
echo failed/total =  $failed/$total 
if [ $total -eq 0 ]
then
 echo no tests ran!
 exit -1
else
 exit $failed
fi

