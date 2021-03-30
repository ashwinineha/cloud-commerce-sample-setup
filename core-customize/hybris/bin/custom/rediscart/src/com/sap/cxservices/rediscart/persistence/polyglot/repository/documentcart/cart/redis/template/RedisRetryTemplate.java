package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;

/**
 * A RedisTemplate that retries on exceptions
 * @param <K> the key
 * @param <V> the value
 */
public class RedisRetryTemplate <K, V> extends RedisTemplate<K, V> {
    private static final Logger LOG = LoggerFactory.getLogger(RedisRetryTemplate.class);

    private final RetryTemplate retryTemplate;

    public RedisRetryTemplate(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
    }

    @Override
    public <T> T execute(final RedisCallback<T> action, final boolean exposeConnection, final boolean pipeline) {
        if (getRetryTemplate() != null) {
            return getRetryTemplate().execute((RetryContext context) -> {
                if (context.getRetryCount() > 0) {
                    LOG.warn("Retry of Redis Operation. Retry Count = {}", context.getRetryCount());
                }
                return super.execute(action, exposeConnection, pipeline);
            });
        }
        return super.execute(action, exposeConnection, pipeline);

    }

    public RetryTemplate getRetryTemplate() {
        return retryTemplate;
    }
}
