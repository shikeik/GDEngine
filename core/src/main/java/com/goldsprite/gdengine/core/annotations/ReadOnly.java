package com.goldsprite.gdengine.core.annotations;
import java.lang.annotation.*;
/** 强制只读 */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
public @interface ReadOnly {}