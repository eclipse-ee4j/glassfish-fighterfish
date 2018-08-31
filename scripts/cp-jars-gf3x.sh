#!/bin/sh -e
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

g=${1}
(cd $g; zip original-osgi-modules.zip modules/osgi-jpa-extension.jar modules/autostart/osgi-*.jar)
cp ./osgi-web-container/target/osgi-web-container.jar ./osgi-cdi/target/osgi-cdi.jar ./osgi-jpa/target/osgi-jpa.jar ./osgi-jta/target/osgi-jta.jar ./osgi-ejb-container/target/osgi-ejb-container.jar ./osgi-http/target/osgi-http.jar ./osgi-jdbc/target/osgi-jdbc.jar ./osgi-javaee-base/target/osgi-javaee-base.jar osgi-ee-resources/target/osgi-ee-resources.jar $g/modules/autostart
cp osgi-jpa-extension/target/osgi-jpa-extension.jar $g/modules/
echo All files successfully copied. Original files are backed up in $g/original-osgi-modules.zip
