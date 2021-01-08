package java.lang.invoke;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import static java.lang.invoke.MethodHandleStatics.*;
import static java.lang.invoke.MethodHandles.Lookup.IMPL_LOOKUP;

class ReflectionSupport {

    public static MethodHandle forMethod(Object method) {
        try {
            return IMPL_LOOKUP.unreflect((Method) method);
        } catch (IllegalAccessException e) {
            throw newInternalError(e);
        }
    }

    public static MethodHandle forConstructor(Object constructor) {
        try {
            return IMPL_LOOKUP.unreflectConstructor((Constructor<?>) constructor);
        } catch (IllegalAccessException e) {
            throw newInternalError(e);
        }
    }

    public static MethodHandle constructorForSerialization(Object constructor,
            Class<?> type) {
        Constructor<?> c = (Constructor<?>) constructor;
        return DirectMethodHandle.makeAllocator(new MemberName(c), type);
    }
    
}
