package com.cloud.backend.util;

import com.cloud.backend.mapper.ShareMapper;

import java.security.SecureRandom;

/**
 * 分享链接短码生成器 —— docs/adr/013。
 *
 * 10 位去混淆字符集（排除 O/0/I/l/1，避免手输混淆），空间 58^10 ≈ 4.3×10^17；
 * 生成时查重（shareToken 唯一索引兜底），冲突则重新生成，最多重试 10 次。
 */
public final class ShareTokenGenerator {

    /** 去混淆字符集：去除 O/0/I/l/1 */
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int TOKEN_LENGTH = 10;
    private static final int MAX_RETRY = 10;

    private static final SecureRandom RANDOM = new SecureRandom();

    private ShareTokenGenerator() {
    }

    public static String generateUniqueToken(ShareMapper shareMapper) {
        for (int i = 0; i < MAX_RETRY; i++) {
            String token = generate();
            if (shareMapper.findByToken(token) == null) {
                return token;
            }
        }
        throw new IllegalStateException("分享 token 生成冲突超过上限");
    }

    private static String generate() {
        StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
