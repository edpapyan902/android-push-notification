/*
 * Copyright 2009, Mahmood Ali.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of Mahmood Ali. nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.notnoop.apns.internal;

import net.sf.json.JSONObject;

import org.junit.Assert;
import org.junit.Test;

public class UtilitiesTest {

    @Test
    public void testEncodeAndDecode() {
        String encodedHex = "a1b2d4";

        byte[] decoded = Utilities.decodeHex(encodedHex);
        String encoded = Utilities.encodeHex(decoded);

        Assert.assertEquals(encodedHex.toLowerCase(), encoded.toLowerCase());
    }

    @Test
    public void simpleClone() {
        JSONObject json = new JSONObject();
        json.put("test", 1);
        json.put("James", "Adams");
        json.put("nullKey", new JSONObject(true));

        JSONObject copy = Utilities.clone(json);
        Assert.assertNotSame(json, copy);
        Assert.assertEquals(json, copy);
    }

    @Test
    public void deepCloning() {
        JSONObject root = new JSONObject();
        root.put("test", 1);

        JSONObject nu = new JSONObject();
        nu.put("mark", "what");
        root.put("nu", nu);

        JSONObject rootClone = Utilities.clone(root);
        JSONObject nuClone = rootClone.getJSONObject("nu");

        Assert.assertNotSame(nu, nuClone);
        Assert.assertEquals(nu, nuClone);
    }
}
