/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.client;

import java.util.List;

public interface Wallet<T extends Identifiable> {
    List<T> list();

    boolean contains(String id);

    T get(String id);

    T add(T value);

    T update(T value);

    T remove(String id);
}
