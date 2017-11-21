/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.driver.hlfv1;

import org.hyperledger.fabric.sdk.User;

class SecurityContext {
    private User user;

    SecurityContext(User user) {
        this.user = user;
    }

    User user() {
        return user;
    }
}
