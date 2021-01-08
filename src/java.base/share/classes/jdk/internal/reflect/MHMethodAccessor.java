package jdk.internal.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Objects;

import jdk.internal.vm.annotation.Stable;

class MHMethodAccessor extends MethodAccessorImpl {

    @Stable
    private final MethodHandle target;
    @Stable
    private final Class<?> owner;
    @Stable
    private final int modifiers;
    
    private static final MethodType MH_TYPE = 
            MethodType.methodType(Object.class, Object.class, Object[].class);
    private static final MethodHandle EX_HANDLER;
    
    static {
        try {
            MethodHandle newInvokationTargetException =
                    MethodHandles.lookup()
                    .findConstructor(InvocationTargetException.class, 
                            MethodType.methodType(void.class, Throwable.class));
            MethodHandle thrower = 
                    MethodHandles.throwException(Object.class, InvocationTargetException.class);
            EX_HANDLER = MethodHandles.filterArguments(thrower, 0, newInvokationTargetException);
        } catch (ReflectiveOperationException e) {
            throw new InternalError(e);
        }
    }

    public MHMethodAccessor(Class<?> owner, MethodHandle target, int modifiers) {
        if (Modifier.isStatic(modifiers)) {
            target = MethodHandles.dropArguments(target, 0, Object.class);
        }
        target = target.asFixedArity();
        target = target.asType(target.type().changeReturnType(Object.class));
        target = MethodHandles.catchException(target, Throwable.class, EX_HANDLER)
            .asSpreader(1, Object[].class, target.type().parameterCount() - 1)
            .asType(MH_TYPE);
        this.target = target;
        this.owner = owner;
        this.modifiers = modifiers;
    }

    @Override
    public Object invoke(Object obj, Object[] args)
            throws IllegalArgumentException, InvocationTargetException {
        if (!Modifier.isStatic(modifiers)) {
            Objects.requireNonNull(obj);
            owner.cast(obj);
        }
        try {
            return target.invokeExact(obj, args);
        } catch (RuntimeException | Error | InvocationTargetException e) {
            throw e;
        } catch (Throwable t) {
            throw new InternalError(t);
        }
    }

}
