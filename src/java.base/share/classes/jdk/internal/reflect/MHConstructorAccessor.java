package jdk.internal.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;

import jdk.internal.vm.annotation.Stable;

public class MHConstructorAccessor extends ConstructorAccessorImpl {

    @Stable
    private final MethodHandle target;
    
    private static final MethodType MH_TYPE = MethodType.methodType(Object.class, Object[].class);
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

    public MHConstructorAccessor(MethodHandle target) {
        target = target.asFixedArity();
        target = target.asType(target.type().changeReturnType(Object.class));
        target = MethodHandles.catchException(target, Throwable.class, EX_HANDLER)
                .asSpreader(Object[].class, target.type().parameterCount())
                .asType(MH_TYPE);
        this.target = target;
    }

    @Override
    public Object newInstance(Object[] args) throws InstantiationException,
            IllegalArgumentException, InvocationTargetException {
        try {
            return target.invokeExact(args);
        } catch (Throwable t) {
            throw new InvocationTargetException(t);
        }
    }

}
