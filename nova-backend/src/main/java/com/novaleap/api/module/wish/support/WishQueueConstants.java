package com.novaleap.api.module.wish.support;

public final class WishQueueConstants {

    public static final String KEY_PREFIX = "nova:wish:";
    public static final String PENDING_QUEUE = KEY_PREFIX + "pending";
    public static final String PENDING_INDEX_SET = KEY_PREFIX + "pending:index";
    public static final String PROCESSING_QUEUE = KEY_PREFIX + "processing:list";
    public static final String PROCESSING_ZSET = KEY_PREFIX + "processing:zset";
    public static final String PROCESSING_LOCK_PREFIX = KEY_PREFIX + "processing:lock:";
    public static final String PROCESSING_RECOVERY_LOCK = KEY_PREFIX + "processing:recover:lock";
    public static final String RETRY_PREFIX = KEY_PREFIX + "retry:";
    public static final String DEAD_LETTER_SET = KEY_PREFIX + "dead";

    private WishQueueConstants() {
    }
}
