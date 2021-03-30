/*
 * [y] SAP Commerce Platform
 *
 *  Copyright (c) 2000-2020 SAP SE. All rights reserved.
 *
 *  This software is the confidential and proprietary information of SAP
 *  Customer Experience ("Confidential Information"). You shall not disclose such
 *  Confidential Information and shall use it only in accordance with the
 *  terms of the license agreement you entered into with SAP Customer Experience.
 */

package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.embedded.RedisServer;

/**
 * The Local redis server.
 */
public class LocalRedisServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRedisServer.class);
    private RedisServer redisServer;
    private int port;
    private boolean enabled;
    private String password;

    /**
     * Sets port.
     *
     * @param port the port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Sets enabled.
     *
     * @param enabled the enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Sets password.
     *
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Init.
     */
    public void init() {
        if (enabled) {
            LOGGER.info("Embedded redis server is enabled. Starting embedded redis server...");
            try {
                redisServer = RedisServer.builder().port(port).setting(String.format("requirepass %s", password)).build();
                redisServer.start();
                LOGGER.info("Embedded redis server started on port: {}", port);
            } catch (Exception exception) {
                LOGGER.error("Unable to start embedded redis server.", exception);
            }
        }
    }

    /**
     * Destroy.
     */
    public void destroy() {
        if (enabled && null != redisServer && redisServer.isActive()) {
            LOGGER.info("Embedded redis server is active. Stopping embedded redis server.");
            try {
                redisServer.stop();
                LOGGER.info("Embedded redis server stopped.");
            } catch (Exception exception) {
                LOGGER.error("Unable to stop embedded redis server.", exception);
            }
        }
    }

}
