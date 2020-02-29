package com.qunar.mybatis.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;
import sun.reflect.generics.reflectiveObjects.TypeVariableImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

@Slf4j
public final class ReflectUtils {

    private static final String GENERIC_PARAM_KEY = "%s[%s]";
    private static final String GENERIC_DECLARATION_PREFIX = "class ";
    private static final List<String> PRIMITIVE_LIST = ImmutableList
            .<String>builder()
            .add(int.class.getName())
            .add(char.class.getName())
            .add(float.class.getName())
            .add(byte.class.getName())
            .add(long.class.getName())
            .add(short.class.getName())
            .add(double.class.getName())
            .add(boolean.class.getName())
            .build();

    public static boolean isPrimitive(String className) {
        return PRIMITIVE_LIST.contains(className);
    }

    /**
     * 获取方法中入参和出参的全部泛型真实信息
     *
     * @param method
     * @return Map key: com.qunar.flight.userproduct.citylist.vo.BaseRequest[T]
     * Map val: 泛型的真实类型
     */
    public static Map<String, Type> getGenericParamNameMap(Method method) {
        Map<String, Type> result = Maps.newHashMap();
        Type returnType = method.getGenericReturnType();
        Type[] paramTypes = method.getGenericParameterTypes();
        result.putAll(getGenericParamNameMap(returnType));
        for (Type type : paramTypes) {
            result.putAll(getGenericParamNameMap(type));
        }
        return result;
    }

    /**
     * 获取继承自泛型类的class的全部泛型真实信息
     * @param clazz
     * @return
     */
    public static Map<String, Type> getGenericParamNameMap(Class clazz) {
        Map<String, Type> result = Maps.newHashMap();
        boolean isGenericSuperclass = clazz.getGenericSuperclass() instanceof ParameterizedTypeImpl;
        if (!isGenericSuperclass) {
            return result;
        }

        Type[] actualTypeArguments = ((ParameterizedTypeImpl) clazz.getGenericSuperclass()).getActualTypeArguments();
        TypeVariable[] typeParameters = clazz.getSuperclass().getTypeParameters();

        for (int i = 0; i < actualTypeArguments.length; i++) {
            Type type = actualTypeArguments[i];
            result.put(getGenericParamKey(typeParameters[i]), type);
        }
        return result;
    }

    private static Map<String, Type> getGenericParamNameMap(Type type) {
        Map<String, Type> result = Maps.newHashMap();
        boolean isGenericSuperclass = type instanceof ParameterizedTypeImpl;
        if (!isGenericSuperclass) {
            return result;
        }
        String rawTypeName = ((ParameterizedTypeImpl) type).getRawType().getName();
        Type[] arguments = ((ParameterizedTypeImpl) type).getActualTypeArguments();
        TypeVariable[] typeParameters = (((ParameterizedTypeImpl) type).getRawType()).getTypeParameters();

        for (int i = 0; i < arguments.length; i++) {
            String parameter = typeParameters[i].getName();
            String key = String.format(GENERIC_PARAM_KEY, rawTypeName, parameter);
            Type argument = arguments[i];
            result.put(key, argument);
        }
        return result;
    }

    /**
     * 获取泛型类的内部key 格式:com.qunar.flight.userproduct.citylist.vo.BaseRequest[T]
     *
     * @param type
     * @return
     */
    public static String getGenericParamKey(Type type) {
        if (!(type instanceof TypeVariableImpl)) {
            return StringUtils.EMPTY;
        }
        TypeVariableImpl typeVariable = (TypeVariableImpl) type;
        String genericDeclarationName = typeVariable.getGenericDeclaration().toString();
        String prefix = genericDeclarationName.replaceFirst(GENERIC_DECLARATION_PREFIX, StringUtils.EMPTY);
        String realName = typeVariable.getName();
        return String.format(GENERIC_PARAM_KEY, prefix, realName);
    }
}
