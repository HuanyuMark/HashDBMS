package org.hashdb.ms.data;

import org.springframework.util.NumberUtils;

import java.util.Objects;

/**
 * Date: 2023/11/22 22:24
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Deprecated
public interface Numeric<T extends Number> {
    T get();

    <O extends Number> Numeric<T> add(O other);

    <O extends Number> Numeric<T> sub(O other);

    <O extends Number> Numeric<T> mul(O other);

    <O extends Number> Numeric<T> div(O other);

/*    static <N extends Number> Numeric<N> of(N num_) {
        Objects.requireNonNull(num_);
        return switch (num_.getClass()) {
            case Integer.class->{
                return new Numeric<Integer>(){
                    private final Integer num = (Integer) num_;
                    @Override
                    public Integer get() {
                        return num;
                    }
                    @Override
                    public <O extends Number> Numeric<Integer> add(O other) {
                        NumberUtils.parseNumber()
                        return null;
                    }

                    @Override
                    public <O extends Number> Numeric<Integer> sub(O other) {
                        return null;
                    }

                    @Override
                    public <O extends Number> Numeric<Integer> mul(O other) {
                        return null;
                    }

                    @Override
                    public <O extends Number> Numeric<Integer> div(O other) {
                        return null;
                    }
                }
            }
        };
    }*/
}
