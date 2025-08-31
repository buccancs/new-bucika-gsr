//package com.topdon.module.thermal.utils;
//
//import java.util.ArrayList;
//
//public class ArrayUtils {
//
//    public static int getMaxIndex(float[] data) {
//        int maxIndex = 0;
//        for (int i = 0; i < data.length - 2; i++) {
//            if (data[i + 1] > data[maxIndex]) {
//                maxIndex = i + 1;
//            }
//        }
//        return maxIndex;
//    }
//
//    public static int getMinIndex(float[] data) {
//        int minIndex = 0;
//        for (int i = 0; i < data.length - 2; i++) {
//            if (data[i + 1] == 0) {
//                continue;
//            }
//            if (data[i + 1] < data[minIndex]) {
//                minIndex = i + 1;
//            }
//        }
//        return minIndex;
//    }
//}
