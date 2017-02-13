/*
 * (c) 2015 Mike Chaberski
 */

package com.github.mike10004.common.net;

/**
 * Class that provides static utility methods relating to rate limits.
 * @author mchaberski
 */
public class RateLimits {

    private RateLimits() {
    }
    public static final int MINUTES_PER_HOUR = 60;
    public static final int MINUTES_PER_SECOND = 60;
    public static final int SECONDS_PER_HOUR = MINUTES_PER_HOUR * MINUTES_PER_SECOND;
    
    public static double perSecondFromPerHour(int permitsPerHour) {
        return (double)permitsPerHour / (double)SECONDS_PER_HOUR;
    }
    
}
