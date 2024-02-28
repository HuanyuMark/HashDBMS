package org.hashdb.ms.data;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Date: 2023/11/23 22:00
 *
 * @author Huanyu Mark
 */
public class BigNumber extends Number implements Comparable<BigNumber> {
    private Number value;

    @Override
    public int intValue() {
        return value.intValue();
    }

    @Override
    public long longValue() {
        return value.intValue();
    }

    @Override
    public float floatValue() {
        return value.intValue();
    }

    @Override
    public double doubleValue() {
        return value.intValue();
    }

    @Override
    public int compareTo(@NotNull BigNumber o) {
//        BigInteger integer = new BigInteger("");


        if (o.value instanceof BigInteger integer2) {
            if (value instanceof BigInteger integer1) {
                return integer1.compareTo(integer2);
            } else {
                BigDecimal decimal1 = (BigDecimal) value;
                // V1 的整数部分
                BigInteger integer1;
//                    try {
//                        decimal1.unscaledValue()
                integer1 = decimal1.toBigInteger();
//                    } catch (ArithmeticException ignore){}
                int delta = integer1.compareTo(integer2);
//                    // 如果整数部分相等, 则比较小数部分
//                    if (delta == 0) {
//                        decimal1.toBigIntegerExact()
//                    }
                return delta < 0 ? delta : -delta;
            }
        } else {
            BigDecimal decimal2 = (BigDecimal) o.value;
            if (value instanceof BigInteger integer1) {
                BigInteger integer2 = decimal2.toBigInteger();
                int delta = integer1.compareTo(integer2);
                return delta < 0 ? delta : -delta;
            } else {
                BigDecimal decimal1 = (BigDecimal) value;
                return decimal1.compareTo(decimal2);
            }
        }

    }
}
