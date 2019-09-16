package com.xuhe.aace;

import org.slf4j.LoggerFactory;

/**
 * @author liq@shinemo.com
 * @version 1.0
 * @date 2019/8/30 16:43
 * @Description
 */
public class Logger {

    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Logger.class);

    public static void ErrLog(String msg) {
        LOGGER.error(msg);
    }

    public static void ErrLog(Throwable e) {
        LOGGER.error("", e);
    }


    public static void InfoLog(String msg) {
        LOGGER.info(msg);
    }

    public static void WarnLog(String msg) {
        LOGGER.warn(msg);
    }
}
