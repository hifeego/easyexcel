package com.alibaba.excel.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import com.alibaba.excel.metadata.NullObject;
import com.alibaba.excel.write.metadata.RowData;

import net.sf.cglib.beans.BeanMap;

/**
 * Field utils
 *
 * @author Jiaju Zhuang
 **/
public class FieldUtils {

    public static Class<?> nullObjectClass = NullObject.class;

    private static final int START_RESOLVE_FIELD_LENGTH = 2;

    public static Class<?> getFieldClass(Map dataMap, String filedName) {
        if (dataMap instanceof BeanMap) {
            return ((BeanMap)dataMap).getPropertyType(filedName);
        }
        Object value = dataMap.get(filedName);
        if (value != null) {
            return value.getClass();
        }
        return nullObjectClass;
    }

    public static Class<?> getFieldClass(RowData rowData, int dataIndex) {
        Object value = rowData.get(dataIndex);
        if (value != null) {
            return value.getClass();
        }
        return nullObjectClass;
    }

    /**
     * Parsing the name matching cglib。
     * <pre>
     *     null -&gt; null
     *     string1 -&gt; string1
     *     String2 -&gt; string2
     *     sTring3 -&gt; STring3
     *     STring4 -&gt; STring4
     *     STRING5 -&gt; STRING5
     *     STRing6 -&gt; STRing6
     * </pre>
     *
     * @param field field
     * @return field name.
     */
    public static String resolveCglibFieldName(Field field) {
        if (field == null) {
            return null;
        }
        String fieldName = field.getName();
        if (StringUtils.isBlank(fieldName) || fieldName.length() < START_RESOLVE_FIELD_LENGTH) {
            return fieldName;
        }
        char firstChar = fieldName.charAt(0);
        char secondChar = fieldName.charAt(1);
        if (Character.isUpperCase(firstChar) == Character.isUpperCase(secondChar)) {
            return fieldName;
        }
        if (Character.isUpperCase(firstChar)) {
            return buildFieldName(Character.toLowerCase(firstChar), fieldName);
        }
        return buildFieldName(Character.toUpperCase(firstChar), fieldName);
    }

    private static String buildFieldName(char firstChar, String fieldName) {
        return firstChar + fieldName.substring(1);
    }

    /**
     * Gets an accessible {@link Field} by name respecting scope. Superclasses/interfaces will be considered.
     *
     * @param cls       the {@link Class} to reflect, must not be {@code null}
     * @param fieldName the field name to obtain
     * @return the Field object
     * @throws IllegalArgumentException if the class is {@code null}, or the field name is blank or empty
     */
    public static Field getField(final Class<?> cls, final String fieldName) {
        final Field field = getField(cls, fieldName, false);
        MemberUtils.setAccessibleWorkaround(field);
        return field;
    }

    /**
     * Gets an accessible {@link Field} by name, breaking scope if requested. Superclasses/interfaces will be
     * considered.
     *
     * @param cls         the {@link Class} to reflect, must not be {@code null}
     * @param fieldName   the field name to obtain
     * @param forceAccess whether to break scope restrictions using the
     *                    {@link java.lang.reflect.AccessibleObject#setAccessible(boolean)} method. {@code false} will
     *                    only
     *                    match {@code public} fields.
     * @return the Field object
     * @throws NullPointerException if the class is {@code null}
     * @throws IllegalArgumentException if the field name is blank or empty or is matched at multiple places
     * in the inheritance hierarchy
     */
    public static Field getField(final Class<?> cls, final String fieldName, final boolean forceAccess) {
        Validate.isTrue(cls != null, "The class must not be null");
        Validate.isTrue(StringUtils.isNotBlank(fieldName), "The field name must not be blank/empty");
        // FIXME is this workaround still needed? lang requires Java 6
        // Sun Java 1.3 has a bugged implementation of getField hence we write the
        // code ourselves

        // getField() will return the Field object with the declaring class
        // set correctly to the class that declares the field. Thus requesting the
        // field on a subclass will return the field from the superclass.
        //
        // priority order for lookup:
        // searchclass private/protected/package/public
        // superclass protected/package/public
        // private/different package blocks access to further superclasses
        // implementedinterface public

        // check up the superclass hierarchy
        for (Class<?> acls = cls; acls != null; acls = acls.getSuperclass()) {
            try {
                final Field field = acls.getDeclaredField(fieldName);
                // getDeclaredField checks for non-public scopes as well
                // and it returns accurate results
                if (!Modifier.isPublic(field.getModifiers())) {
                    if (forceAccess) {
                        field.setAccessible(true);
                    } else {
                        continue;
                    }
                }
                return field;
            } catch (final NoSuchFieldException ex) { // NOPMD
                // ignore
            }
        }
        // check the public interface case. This must be manually searched for
        // incase there is a public supersuperclass field hidden by a private/package
        // superclass field.
        Field match = null;
        for (final Class<?> class1 : ClassUtils.getAllInterfaces(cls)) {
            try {
                final Field test = class1.getField(fieldName);
                Validate.isTrue(match == null, "Reference to field %s is ambiguous relative to %s"
                    + "; a matching field exists on two or more implemented interfaces.", fieldName, cls);
                match = test;
            } catch (final NoSuchFieldException ex) { // NOPMD
                // ignore
            }
        }
        return match;
    }

}
