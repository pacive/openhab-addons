/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mitsubishiheatpump.internal.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.HttpProtocolException;

@NonNullByDefault
public class CryptoHelper {
    private static final Key DEFAULT_KEY = new SecretKeySpec(
            new byte[] { 'u', 'n', 'r', 'e', 'g', 'i', 's', 't', 'e', 'r', 'e', 'd', 0, 0, 0, 0 }, "AES");

    private final Cipher cipher;

    public CryptoHelper() {
        Security.addProvider(new BouncyCastleProvider());
        try {
            this.cipher = Cipher.getInstance("AES/CBC/ISO7816-4Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new ExceptionInInitializerError("Unable to initialize cryptography instance");
        }
    }

    public synchronized String encryptPayload(String data) throws HttpProtocolException {
        byte[] encrypted = encryptData(data.getBytes(StandardCharsets.UTF_8));
        return toBase64String(encrypted);
    }

    private synchronized byte[] encryptData(byte[] data) throws HttpProtocolException {
        try {
            this.cipher.init(Cipher.ENCRYPT_MODE, DEFAULT_KEY);
            return Util.concatenate(this.cipher.getIV(), this.cipher.doFinal(data));
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new HttpProtocolException("Unable to encrypt content", e);
        }
    }

    public synchronized byte[] decryptPayload(String data) throws HttpProtocolException {
        byte[] decoded = fromBase64String(data);
        return decryptData(decoded);
    }

    private synchronized byte[] decryptData(byte[] data) throws HttpProtocolException {
        try {
            byte[] iv = Arrays.copyOfRange(data, 0, 16);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            this.cipher.init(Cipher.DECRYPT_MODE, DEFAULT_KEY, ivSpec);
            return this.cipher.doFinal(Arrays.copyOfRange(data, 16, data.length));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException
                | BadPaddingException e) {
            throw new HttpProtocolException("Unable to decrypt content", e);
        }
    }

    public String toBase64String(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public byte[] fromBase64String(String data) {
        return Base64.getDecoder().decode(data);
    }
}
