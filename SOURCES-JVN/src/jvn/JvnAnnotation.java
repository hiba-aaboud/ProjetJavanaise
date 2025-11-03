package jvn;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

public @interface JvnAnnotation {

    String operation();
}
