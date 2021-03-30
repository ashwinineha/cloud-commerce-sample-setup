package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import redis.clients.jedis.Jedis;

/**
 * A JedisConnectionFactory that allows the cluster and non-cluster setup controlled with a flag.
 */
public class ConditionalJedisConnectionFactory extends JedisConnectionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ConditionalJedisConnectionFactory.class);

    private boolean clusterEnabled;

    private RetryTemplate retryTemplate;

    public ConditionalJedisConnectionFactory(RedisClusterConfiguration clusterConfig, JedisClientConfiguration clientConfig) {
        super(clusterConfig, clientConfig);
    }

    @Override
    public boolean isRedisClusterAware() {
        LOG.debug("Cluster is {}abled for Redis.", clusterEnabled ? "en" : "dis");
        if (clusterEnabled) {
            return super.isRedisClusterAware();
        } else {
            return false;
        }
    }

    /**
     * Returns a Jedis instance to be used as a Redis connection. The instance can be newly created or retrieved from a pool.
     * When there is an exception connecting it will be retried according to the {@code retryTemplate}.
     *
     * @return Jedis instance ready for wrapping into a {@link org.springframework.data.redis.connection.RedisConnection}.
     */
    @Override
    protected Jedis fetchJedisConnector() {
        if (retryTemplate != null) {
            return retryTemplate.execute((RetryContext context) -> {
                if (context.getRetryCount() > 0) {
                    LOG.warn("Retrying Redis connection. Retry Count = {}", context.getRetryCount());
                }
                return super.fetchJedisConnector();
            });
        }
        return super.fetchJedisConnector();
    }

    public void setClusterEnabled(boolean clusterEnabled) {
        this.clusterEnabled = clusterEnabled;
    }

    public void setRetryTemplate(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
    }
}
