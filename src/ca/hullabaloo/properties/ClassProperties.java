package ca.hullabaloo.properties;

import net.sf.cglib.proxy.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ca.hullabaloo.properties.Utils.checkArgument;

class ClassProperties<T> {
    public static <T> T create(Resolver props, Class<T> type) {
        validateType(type);
        return new ClassProperties<T>(props, type).createInstance();
    }

    private static <T> void validateType(Class<T> type) {
        if (!Modifier.isPublic(type.getModifiers()) || Modifier.isFinal(type.getModifiers()))
            throw JavaPropertyException.nonPublicOrFinalClass(type);
        if (type.getEnclosingClass() != null && !Modifier.isStatic(type.getModifiers()))
            throw JavaPropertyException.nonStaticInnerClass(type);
    }

    private final Resolver props;

    private final Class<T> type;

    private ClassProperties(Resolver props, Class<T> type) {
        this.props = props;
        this.type = type;
    }

    private T createInstance() {

        List<Callback> callbacks = new ArrayList<Callback>();
        callbacks.add(NoOp.INSTANCE);
        Map<Method, Integer> callbackIndex = new HashMap<Method, Integer>();
        for (Method m : this.type.getDeclaredMethods()) {
            checkArgument(m.getReturnType() == String.class);
            checkArgument(m.getParameterTypes().length == 0);
            int modifiers = m.getModifiers();
            boolean shouldOverride = Modifier.isPublic(modifiers) && !Modifier.isFinal(modifiers);
            if (!shouldOverride) {
                if (Modifier.isAbstract(modifiers))
                    throw JavaPropertyException.nonPublicAbstractMethod(m);
                callbackIndex.put(m, 0);
            } else {
                String prop = Utils.propertyName(m);
                String value = this.props.resolve(prop);
                if (value == null) {
                    if (Modifier.isAbstract(modifiers))
                        throw JavaPropertyException.missingProperty(prop, m);
                    callbackIndex.put(m, 0);
                } else {
                    callbackIndex.put(m, callbacks.size());
                    callbacks.add(new FixedValueImpl(value));
                }
            }
        }
        Enhancer e = new Enhancer();
        e.setCallbacks(callbacks.toArray(new Callback[callbacks.size()]));
        e.setCallbackFilter(new MapCallbackFilter(callbackIndex));
        e.setSuperclass(this.type);
        e.setUseFactory(false);
        e.setUseCache(false);
        return castToType(e.create());
    }

    @SuppressWarnings({"unchecked"})
    private T castToType(Object o) {
        return (T) o;
    }

    private class FixedValueImpl implements FixedValue {
        private final String value;

        public FixedValueImpl(String value) {
            this.value = value;
        }

        public Object loadObject() throws Exception {
            return this.value;
        }
    }

    private class MapCallbackFilter implements CallbackFilter {
        private final Map<Method, Integer> callbackIndexes;

        public MapCallbackFilter(Map<Method, Integer> callbackIndexes) {
            this.callbackIndexes = callbackIndexes;
        }

        public int accept(Method method) {
            Integer i = this.callbackIndexes.get(method);
            return i == null ? 0 : i;
        }
    }
}
