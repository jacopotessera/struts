/*
 * Copyright 2002-2006,2009 The Apache Software Foundation.
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opensymphony.xwork2.util;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>AnnotationUtils</code>
 * <p>
 * Various utility methods dealing with annotations
 * </p>
 *
 * @author Rainer Hermanns
 * @author Zsolt Szasz, zsolt at lorecraft dot com
 * @author Dan Oxlade, dan d0t oxlade at gmail d0t c0m
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Fisher
 * @author Chris Beams
 * @author Phillip Webb
 * @version $Id$
 */
public class AnnotationUtils {

    private static final Pattern SETTER_PATTERN = Pattern.compile("set([A-Z][A-Za-z0-9]*)$");
    private static final Pattern GETTER_PATTERN = Pattern.compile("(get|is|has)([A-Z][A-Za-z0-9]*)$");

    private static final Map<AnnotationCacheKey, Annotation> findAnnotationCache =
            new ConcurrentHashMap<AnnotationCacheKey, Annotation>(256);

    private static final Map<Class<?>, Boolean> annotatedInterfaceCache =
            new ConcurrentHashMap<Class<?>, Boolean>(256);

    /**
     * Adds all fields with the specified Annotation of class clazz and its superclasses to allFields
     *
     * @param annotationClass the {@link Annotation}s to find
     * @param clazz           The {@link Class} to inspect
     * @param allFields       list of all fields
     */
    public static void addAllFields(Class<? extends Annotation> annotationClass, Class clazz, List<Field> allFields) {

        if (clazz == null) {
            return;
        }

        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            Annotation ann = field.getAnnotation(annotationClass);
            if (ann != null) {
                allFields.add(field);
            }
        }
        addAllFields(annotationClass, clazz.getSuperclass(), allFields);
    }

    /**
     * Adds all methods with the specified Annotation of class clazz and its superclasses to allFields
     *
     * @param annotationClass the {@link Annotation}s to find
     * @param clazz           The {@link Class} to inspect
     * @param allMethods      list of all methods
     */
    public static void addAllMethods(Class<? extends Annotation> annotationClass, Class clazz, List<Method> allMethods) {

        if (clazz == null) {
            return;
        }

        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            Annotation ann = method.getAnnotation(annotationClass);
            if (ann != null) {
                allMethods.add(method);
            }
        }
        addAllMethods(annotationClass, clazz.getSuperclass(), allMethods);
    }

    /**
     * @param clazz         The {@link Class} to inspect
     * @param allInterfaces list of all interfaces
     */
    public static void addAllInterfaces(Class clazz, List<Class> allInterfaces) {
        if (clazz == null) {
            return;
        }

        Class[] interfaces = clazz.getInterfaces();
        allInterfaces.addAll(Arrays.asList(interfaces));
        addAllInterfaces(clazz.getSuperclass(), allInterfaces);
    }

    /**
     * For the given <code>Class</code> get a collection of the the {@link AnnotatedElement}s
     * that match the given <code>annotation</code>s or if no <code>annotation</code>s are
     * specified then return all of the annotated elements of the given <code>Class</code>.
     * Includes only the method level annotations.
     *
     * @param clazz      The {@link Class} to inspect
     * @param annotation the {@link Annotation}s to find
     * @return A {@link Collection}&lt;{@link AnnotatedElement}&gt; containing all of the
     * method {@link AnnotatedElement}s matching the specified {@link Annotation}s
     */
    public static Collection<Method> getAnnotatedMethods(Class clazz, final Class<? extends Annotation>... annotation) {
        final Collection<Method> toReturn = new HashSet<>();

        ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException {
                if (ArrayUtils.isEmpty(annotation) && ArrayUtils.isNotEmpty(method.getAnnotations())) {
                    toReturn.add(method);
                    return;
                }
                for (Class<? extends Annotation> c : annotation) {
                    if (null != findAnnotation(method, c)) {
                        toReturn.add(method);
                        break;
                    }
                }
            }
        });

        return toReturn;
    }

    /**
     * Find a single {@link Annotation} of {@code annotationType} from the supplied
     * {@link Method}, traversing its super methods (i.e., from superclasses and
     * interfaces) if no annotation can be found on the given method itself.
     * <p>Annotations on methods are not inherited by default, so we need to handle
     * this explicitly.
     *
     * @param method         the method to look for annotations on
     * @param annotationType the annotation type to look for
     * @return the annotation found, or {@code null} if none
     */
    public static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
        AnnotationCacheKey cacheKey = new AnnotationCacheKey(method, annotationType);
        A result = (A) findAnnotationCache.get(cacheKey);
        if (result == null) {
            result = getAnnotation(method, annotationType);
            Class<?> clazz = method.getDeclaringClass();
            if (result == null) {
                result = searchOnInterfaces(method, annotationType, clazz.getInterfaces());
            }
            while (result == null) {
                clazz = clazz.getSuperclass();
                if (clazz == null || clazz.equals(Object.class)) {
                    break;
                }
                try {
                    Method equivalentMethod = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
                    result = getAnnotation(equivalentMethod, annotationType);
                } catch (NoSuchMethodException ex) {
                    // No equivalent method found
                }
                if (result == null) {
                    result = searchOnInterfaces(method, annotationType, clazz.getInterfaces());
                }
            }
            if (result != null) {
                findAnnotationCache.put(cacheKey, result);
            }
        }
        return result;
    }

    /**
     * Get a single {@link Annotation} of {@code annotationType} from the supplied
     * Method, Constructor or Field. Meta-annotations will be searched if the annotation
     * is not declared locally on the supplied element.
     *
     * @param annotatedElement the Method, Constructor or Field from which to get the annotation
     * @param annotationType   the annotation type to look for, both locally and as a meta-annotation
     * @return the matching annotation, or {@code null} if none found
     */
    public static <T extends Annotation> T getAnnotation(AnnotatedElement annotatedElement, Class<T> annotationType) {
        try {
            T ann = annotatedElement.getAnnotation(annotationType);
            if (ann == null) {
                for (Annotation metaAnn : annotatedElement.getAnnotations()) {
                    ann = metaAnn.annotationType().getAnnotation(annotationType);
                    if (ann != null) {
                        break;
                    }
                }
            }
            return ann;
        } catch (Exception ex) {
            // Assuming nested Class values not resolvable within annotation attributes...
            return null;
        }
    }

    private static <A extends Annotation> A searchOnInterfaces(Method method, Class<A> annotationType, Class<?>... ifcs) {
        A annotation = null;
        for (Class<?> iface : ifcs) {
            if (isInterfaceWithAnnotatedMethods(iface)) {
                try {
                    Method equivalentMethod = iface.getMethod(method.getName(), method.getParameterTypes());
                    annotation = getAnnotation(equivalentMethod, annotationType);
                } catch (NoSuchMethodException ex) {
                    // Skip this interface - it doesn't have the method...
                }
                if (annotation != null) {
                    break;
                }
            }
        }
        return annotation;
    }

    private static boolean isInterfaceWithAnnotatedMethods(Class<?> iface) {
        Boolean flag = annotatedInterfaceCache.get(iface);
        if (flag != null) {
            return flag;
        }
        boolean found = false;
        for (Method ifcMethod : iface.getMethods()) {
            try {
                if (ifcMethod.getAnnotations().length > 0) {
                    found = true;
                    break;
                }
            } catch (Exception ex) {
                // Assuming nested Class values not resolvable within annotation attributes...
            }
        }
        annotatedInterfaceCache.put(iface, found);
        return found;
    }

    /**
     * Returns the property name for a method.
     * This method is independent from property fields.
     *
     * @param method The method to get the property name for.
     * @return the property name for given method; null if non could be resolved.
     */
    public static String resolvePropertyName(Method method) {

        Matcher matcher = SETTER_PATTERN.matcher(method.getName());
        if (matcher.matches() && method.getParameterTypes().length == 1) {
            String raw = matcher.group(1);
            return raw.substring(0, 1).toLowerCase() + raw.substring(1);
        }

        matcher = GETTER_PATTERN.matcher(method.getName());
        if (matcher.matches() && method.getParameterTypes().length == 0) {
            String raw = matcher.group(2);
            return raw.substring(0, 1).toLowerCase() + raw.substring(1);
        }

        return null;
    }

    /**
     * Returns the annotation on the given class or the package of the class. This searchs up the
     * class hierarchy and the package hierarchy for the closest match.
     *
     * @param <T>             class type
     * @param clazz           The class to search for the annotation.
     * @param annotationClass The Class of the annotation.
     * @return The annotation or null.
     */
    public static <T extends Annotation> T findAnnotation(Class<?> clazz, Class<T> annotationClass) {
        AnnotationCacheKey cacheKey = new AnnotationCacheKey(clazz, annotationClass);
        T ann = (T) findAnnotationCache.get(cacheKey);
        if (ann == null) {
            ann = clazz.getAnnotation(annotationClass);
            while (ann == null && clazz != null) {
                ann = clazz.getAnnotation(annotationClass);
                if (ann == null) {
                    ann = clazz.getPackage().getAnnotation(annotationClass);
                }
                if (ann == null) {
                    clazz = clazz.getSuperclass();
                    if (clazz != null) {
                        ann = clazz.getAnnotation(annotationClass);
                    }
                }
            }
            if (ann != null) {
                findAnnotationCache.put(cacheKey, ann);
            }
        }

        return ann;
    }


    /**
     * Cache key for the AnnotatedElement cache.
     */
    private static class AnnotationCacheKey {

        private final AnnotatedElement element;

        private final Class<? extends Annotation> annotationType;

        public AnnotationCacheKey(AnnotatedElement element, Class<? extends Annotation> annotationType) {
            this.element = element;
            this.annotationType = annotationType;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof AnnotationCacheKey)) {
                return false;
            }
            AnnotationCacheKey otherKey = (AnnotationCacheKey) other;
            return (this.element.equals(otherKey.element) &&
                    this.annotationType.equals(otherKey.annotationType));
        }

        @Override
        public int hashCode() {
            return (this.element.hashCode() * 29 + this.annotationType.hashCode());
        }
    }
}
