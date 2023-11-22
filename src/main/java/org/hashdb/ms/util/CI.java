package org.hashdb.ms.util;

import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.stream.IntStream;

/**
 * Date: 2023/11/14 13:25
 * 标准命令行输入 工具类 StandardCmdInput
 */
public class CI {
    private final static Scanner scanner = new Scanner(System.in);
    public static String[] multiStrings(int count) {
        return IntStream.range(0,count).mapToObj((n)->singleString()).toArray(String[]::new);
    }
    public static String singleString() {
        return scanner.next();
    }
    public static double[] multiDoubles(int count) {
        return IntStream.range(0,count).mapToDouble((n)->singleDouble()).toArray();
    }
    public static double singleDouble() {
        try {
            return scanner.nextDouble();
        } catch (InputMismatchException e) {
            LErr("[Format Error]: Check you format, input again:");
            // 跳过非法输入
            singleString();
            return singleDouble();
        }
    }
    public static void formatTableHeader(String[] columnNames) {
        formatTableHeader(columnNames,"\t");
    }
    public static void formatTableHeader(String[] columnNames, String separator) {
        String text = String.join(separator, columnNames);
        System.out.println(text);
    }
    public static void LOut(String msg){
        System.out.println(msg);
    }
    public static void LErr(String msg) {
        System.err.println(msg);
    }

    public static void br(){
        LOut("--------");
    }
}
