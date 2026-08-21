// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Brandon Scott

package com.xeonproductions.craftbookultimate.core.control;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The passwords guarding the switches that are not open to everyone.
 *
 * <p>A password is never kept. What is kept is a salted, deliberately slow hash of it, so somebody
 * who reads the file this is saved to learns nothing they can type back in, and cannot work
 * through a list of likely passwords quickly either.
 *
 * <p>Checking a password takes a measurable fraction of a second by design. That is the point of
 * the construction, and it is why callers should check off the thread that ticks the world.
 *
 * <p>Safe to use from any number of threads at once.
 */
@NullMarked
public final class PasswordStore {

    /** The key derivation this store uses. */
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /** How many rounds of hashing stand between a guess and an answer. */
    private static final int ITERATIONS = 100_000;

    /** How many bits of hash to keep. */
    private static final int KEY_BITS = 256;

    /** How many bytes of salt each password gets. */
    private static final int SALT_BYTES = 16;

    /** Separates the parts of a saved entry. */
    private static final char FIELD_SEPARATOR = ':';

    private static final Base64.Encoder ENCODER = Base64.getEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Secret> secrets = new ConcurrentHashMap<>();

    /**
     * One password as it is kept: never the password itself.
     *
     * @param salt random bytes mixed in, so two people choosing the same password do not end up
     *     with the same hash
     * @param hash what the password and salt derive to
     */
    private record Secret(byte[] salt, byte[] hash) {}

    /** Whether a name has a password on it. */
    public boolean hasPassword(String name) {
        return secrets.containsKey(name);
    }

    /**
     * Whether a password is the one set for a name.
     *
     * <p>A name with no password matches nothing, so a switch cannot be opened by guessing that it
     * is unguarded.
     */
    public boolean matches(String name, String password) {
        Secret secret = secrets.get(name);
        if (secret == null) {
            return false;
        }
        // Compared in constant time, so how long a check takes says nothing about how much of the
        // password was right.
        return MessageDigest.isEqual(secret.hash(), derive(password, secret.salt()));
    }

    /**
     * Puts a password on a name that has none.
     *
     * @return true if the password was set
     */
    public boolean setPassword(String name, String password) {
        if (password.isEmpty() || secrets.containsKey(name)) {
            return false;
        }
        secrets.put(name, newSecret(password));
        return true;
    }

    /**
     * Replaces the password on a name, which takes knowing the old one.
     *
     * @return true if the old password was right and the new one is now in force
     */
    public boolean changePassword(String name, String oldPassword, String newPassword) {
        if (newPassword.isEmpty() || !matches(name, oldPassword)) {
            return false;
        }
        secrets.put(name, newSecret(newPassword));
        return true;
    }

    /** Takes the password off a name entirely. */
    public boolean removePassword(String name) {
        return secrets.remove(name) != null;
    }

    /** Every name with a password on it, in order. */
    public List<String> names() {
        return secrets.keySet().stream().sorted().toList();
    }

    /** The number of names with passwords. */
    public int size() {
        return secrets.size();
    }

    /** Forgets every password. */
    public void clear() {
        secrets.clear();
    }

    /**
     * Every password as a line of text, for saving.
     *
     * <p>A line is the name, the salt and the hash, so nothing that could be typed back in appears
     * anywhere. Names containing the separator are not written, since they could not be read back.
     */
    public List<String> save() {
        return secrets.entrySet().stream()
                .filter(entry -> entry.getKey().indexOf(FIELD_SEPARATOR) < 0)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey()
                        + FIELD_SEPARATOR + ENCODER.encodeToString(entry.getValue().salt())
                        + FIELD_SEPARATOR + ENCODER.encodeToString(entry.getValue().hash()))
                .toList();
    }

    /**
     * Reads back what {@link #save()} wrote, adding to whatever is already held.
     *
     * <p>A line that cannot be read is skipped rather than stopping the rest, so one damaged entry
     * does not lock everybody out.
     *
     * @return how many passwords were read
     */
    public int load(Collection<String> lines) {
        int read = 0;
        for (String line : lines) {
            Secret secret = null;
            String[] parts = line.trim().split(String.valueOf(FIELD_SEPARATOR));
            if (parts.length == 3 && !parts[0].isEmpty()) {
                secret = decode(parts[1], parts[2]);
            }
            if (secret != null) {
                secrets.put(parts[0], secret);
                read++;
            }
        }
        return read;
    }

    private static @Nullable Secret decode(String salt, String hash) {
        try {
            return new Secret(DECODER.decode(salt), DECODER.decode(hash));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Secret newSecret(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return new Secret(salt, derive(password, salt));
    }

    private static byte[] derive(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
            try {
                return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Every Java runtime is required to provide this, so its absence is not something a
            // caller could sensibly recover from.
            throw new IllegalStateException("This runtime cannot derive keys with " + ALGORITHM, e);
        }
    }

    /** The character that may not appear in a name that is to be saved. */
    public static boolean isSaveableName(String name) {
        return !name.isEmpty() && name.indexOf(FIELD_SEPARATOR) < 0;
    }

    /** How many bytes of a password's derivation are kept. */
    public static int hashBytes() {
        return KEY_BITS / Byte.SIZE;
    }
}
