package com.kpmg.rcm.sourcing.common.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.kpmg.rcm.sourcing.common.dto.MemoryManagement;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Harsh Goswami
 */
@SuppressWarnings("rawtypes")
@Slf4j
public class MemoryUtil {

    // 10 seconds
    private static final int WAIT_FOR_GC_IN_MILLIS = 10000;

    public static void clear(Collection... collections) {
        if (collections == null || collections.length <= 0)
            return;

        for (Collection collection : collections) {
            if (collection != null) {

				try {
					Iterator iterator = collection.iterator();
					while (iterator.hasNext()) {
						Object type = iterator.next();
						if (type instanceof MemoryManagement) {
							MemoryManagement object = (MemoryManagement) type;
							clear(object);
						} else
							break;
					}
				} catch (Exception e) {
					log.warn("Error clearing collection : " + ExceptionUtils.getStackTrace(e));
				}

                try {
                    collection.clear();
                    collection = null;
                } catch (Exception e) {
                    log.warn("Error clearing collection : " + ExceptionUtils.getStackTrace(e));
                }
            }
        }
        printAndWait();
    }

    public static void clear(Map... maps) {
        if (maps == null || maps.length <= 0)
            return;

        for (Map map : maps) {
            if (map != null) {
                try {
                    map.clear();
                    map = null;
                } catch (Exception e) {
                    log.warn("Error clearing Map : " + ExceptionUtils.getStackTrace(e));
                }
            }
        }
        printAndWait();
    }

    public static void clear(MemoryManagement<?>... objects) {
        if (objects == null || objects.length <= 0)
            return;

        for (MemoryManagement<?> object : objects) {
            if (object != null) {
                try {
                    object.clear();
                    // TODO Make Object null ?
                } catch (Exception e) {
                    log.warn("Error clearing Object : " + ExceptionUtils.getStackTrace(e));
                }
            }
        }
        printAndWait();
    }

    public static void printAndWait() {

        // Memory in Bytes
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();

        long allocatedMemory = totalMemory - freeMemory;
        long presumableFreeMemory = maxMemory - allocatedMemory;

        // Memory in MBs
        long totalMemoryInMB = totalMemory / 1024 / 1024;
        long freeMemoryInMB = freeMemory / 1024 / 1024;
        long maxMemoryInMB = maxMemory / 1024 / 1024;

        long allocatedMemoryInMB = totalMemoryInMB - freeMemoryInMB;
        long presumableFreeMemoryInMB = presumableFreeMemory / 1024 / 1024;

//        log.info(
//                "totalMemory: " + totalMemoryInMB + " MB, freeMemory: " + freeMemoryInMB + " MB, Used Memory: "
//                        + allocatedMemoryInMB + " MB, maxMemory: "
//                        + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemoryInMB) + " MB, presumableFreeMemory: "
//                        + presumableFreeMemoryInMB + " MB");

        double freeMemoryThresholdInMB = totalMemoryInMB * 0.30;
        // If Free Memory is less than 30% threshold of total Memory then wait for
        // garbage collection
        if (freeMemoryInMB < freeMemoryThresholdInMB) {
//            try {
                log.warn(
                        "totalMemory: " + totalMemoryInMB + " MB, freeMemory: " + freeMemoryInMB + " MB, Used Memory: "
                                + allocatedMemoryInMB + " MB, maxMemory: "
                                + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemoryInMB) + " MB, presumableFreeMemory: "
                                + presumableFreeMemoryInMB + " MB, freeMemoryThresholdInMB: " + freeMemoryThresholdInMB + ", freeMemoryInMB("
                                + freeMemoryInMB + ") < freeMemoryThresholdInMB(" + freeMemoryThresholdInMB + ") = "
                                + (freeMemoryInMB < freeMemoryThresholdInMB));
//                System.gc();
//                Thread.sleep(WAIT_FOR_GC_IN_MILLIS);
//            } catch (InterruptedException e) {
//                log.warn("Error in freeing Memory Thread.sleep for 10 seconds Exception: "
//                        + ExceptionUtils.getStackTrace(e));
//            }
        }
    }

    public static void main(String[] args) {

        printAndWait();

//		 for (int i = 0; i < 1000; i++) {
////			Vector v = new Vector(214444444);
////			Vector v1 = new Vector(214744444);
////			Vector v2 = new Vector(214444444);
//			long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
//			long freeMemory = Runtime.getRuntime().freeMemory()  / 1024 / 1024;
//			System.out.println("Runtime.getRuntime().totalMemory() : " + totalMemory + " MB");
//			System.out.println("Runtime.getRuntime().freeMemory() : " + freeMemory + " MB");
//
//			System.out.println(totalMemory - freeMemory + " MB");
//			System.out.println("==========================");
//		}
    }

}
