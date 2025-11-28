package com.wave.hextractor.pojo;

/**
 * File with digests.
 * @author slcantero
 */
public record FileWithDigests(String name, byte[] bytes, String md5, String sha1, String crc32) {
}
