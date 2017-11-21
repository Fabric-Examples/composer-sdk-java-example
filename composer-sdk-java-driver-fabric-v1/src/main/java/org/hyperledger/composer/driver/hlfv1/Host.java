/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.driver.hlfv1;

import java.io.Serializable;
import java.util.Properties;

class Host implements Serializable {
    public String name;
    public String url;
    public Properties properties;

	// for json construct
	public Host() {}

    Host(String name, String url, Properties properties) {
	    this();
        this.name = name;
        this.url = url;
        this.properties = properties;
    }

    @Override
    public String toString() {
        return '{' + name + ':' + url + '}';
    }
}
