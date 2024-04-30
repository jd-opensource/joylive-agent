/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.core.util.type.generic;

import lombok.Getter;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Contains the generic type information of a class.
 * This class holds information about a class's generic type parameters, including those of its superclass, fields, and methods.
 */
public class GenericType {
    /**
     * The class whose generic type information is being represented.
     */
    @Getter
    protected Class<?> type;
    /**
     * A map holding the generic type information of the superclass.
     */
    protected Map<Class<?>, Generic> typeGenerics = new HashMap<>();
    /**
     * A map holding the generic type information of fields.
     */
    protected Map<Field, Generic> fieldGenerics = new ConcurrentHashMap<>();
    /**
     * A map holding the generic type information of executables (methods and constructors).
     */
    protected Map<Executable, GenericExecutable<?>> executableGenerics = new ConcurrentHashMap<>();

    /**
     * Constructs a {@code GenericType} instance for the specified class.
     *
     * @param type The class whose generic type information is to be represented.
     */
    public GenericType(Class<?> type) {
        this(type, null);
    }

    /**
     * Constructs a {@code GenericType} instance for the specified class and its parent's type map.
     *
     * @param type   The class whose generic type information is to be represented.
     * @param parent A map of the parent class's type information, if any.
     */
    public GenericType(Class<?> type, Map<String, ? extends Type> parent) {
        this.type = type;

        Generic childType = new Generic(type, type);
        // The generic type declarations of the current type
        TypeVariable<? extends Class<?>>[] variables = type.getTypeParameters();
        if (variables.length > 0) {
            for (TypeVariable<? extends Class<?>> variable : variables) {
                if (parent == null) {
                    childType.addVariable(new GenericVariable(variable.getName(), variable.getBounds()[0]));
                } else {
                    childType.addVariable(new GenericVariable(variable.getName(), parent.get(variable.getName())));
                }
            }
            typeGenerics.put(type, childType);
        }
        // Check if it is an interface
        if (!type.isInterface()) {
            // If it's not an interface, iterate over superclasses
            Class<?> clazz = type;
            Class<?> parentClazz;
            while (clazz != null && clazz != Object.class) {
                parentClazz = clazz.getSuperclass();
                childType = parentType(parentClazz, clazz.getGenericSuperclass(), childType);
                clazz = parentClazz;
            }
        } else {
            // If it's an interface
            Set<Class<?>> uniques = new HashSet<>(10);
            Class<?>[] ifaces = type.getInterfaces();
            Type[] gfaces = type.getGenericInterfaces();
            for (int i = 0; i < ifaces.length; i++) {
                // Recursively traverse parent interfaces
                interfaceType(ifaces[i], gfaces[i], childType, uniques);
            }
        }
    }

    /**
     * Builds the generic object for a superclass.
     *
     * @param iface     The current interface class.
     * @param gface     The generic type of the superclass as seen from the subclass.
     * @param childType The generic type of the subclass.
     * @param uniques   A set for handling unique interfaces.
     */
    protected void interfaceType(final Class<?> iface, final Type gface, final Generic childType, final Set<Class<?>> uniques) {
        Generic parentType = parentType(iface, gface, childType);
        Class<?>[] ifaces = iface.getInterfaces();
        Type[] gfaces = iface.getGenericInterfaces();
        for (int i = 0; i < ifaces.length; i++) {
            if (uniques.add(ifaces[i])) {
                interfaceType(ifaces[i], gfaces[i], parentType, uniques);
            }
        }
    }

    /**
     * Builds the generic type for a superclass.
     *
     * @param parentCls  The superclass.
     * @param parentType The generic type of the superclass as seen from the subclass.
     * @param childType  The generic type of the subclass.
     * @return A {@code Generic} object representing the superclass's generic type.
     */
    /**
     * Determines the parent type's generic information and constructs a Generic object for it.
     *
     * @param parentCls  The class of the parent.
     * @param parentType The type of the parent, including generic information.
     * @param childType  The child type's Generic object, used for determining actual type arguments.
     * @return A new Generic object representing the parent's type information.
     */
    protected Generic parentType(final Class<?> parentCls, final Type parentType, final Generic childType) {
        Generic result = new Generic(parentType, parentCls);
        // The generic type variables of the parent class
        TypeVariable<? extends Class<?>>[] variables = parentCls.getTypeParameters();
        // Using the child class to determine the specific types for parent's generics
        if (parentType instanceof ParameterizedType) {
            // Get the class type objects within the generic
            Type[] arguments = ((ParameterizedType) parentType).getActualTypeArguments();
            Type argument;
            String name;
            for (int i = 0; i < arguments.length; i++) {
                argument = arguments[i];
                name = variables[i].getName();
                if (argument instanceof Class) {
                    // Class type argument
                    result.addVariable(new GenericVariable(name, argument));
                } else if (argument instanceof TypeVariable) {
                    // Retrieve the generic definition from the child class
                    result.addVariable(new GenericVariable(name, childType.getVariable(((TypeVariable<?>) argument).getName()).getType()));
                } else if (argument instanceof ParameterizedType) {
                    result.addVariable(new GenericVariable(name, compute(argument, null, childType).getType()));
                } else if (argument instanceof GenericArrayType) {
                    result.addVariable(new GenericVariable(name, compute(argument, null, childType).getType()));
                } else if (argument instanceof WildcardType) {
                    result.addVariable(new GenericVariable(name, compute(argument, null, childType).getType()));
                }
            }
        }
        // Store the parent class's generic information
        typeGenerics.put(parentCls, result);
        return result;
    }

    /**
     * Gets the generic type information for a field.
     *
     * @param field The field for which to get the generic type information.
     * @return The generic type information for the field, or {@code null} if the field is {@code null}.
     */
    public Generic get(final Field field) {
        return field == null ? null : fieldGenerics.computeIfAbsent(field, key -> compute(field));
    }

    /**
     * Gets the generic type information for a constructor.
     *
     * @param constructor The constructor for which to get the generic type information.
     * @return The generic type information for the constructor, or {@code null} if the constructor is {@code null}.
     */
    public GenericConstructor get(final Constructor<?> constructor) {
        return constructor == null ? null : (GenericConstructor) executableGenerics.computeIfAbsent(constructor, key -> compute(constructor));
    }

    /**
     * Gets the generic type information for a method.
     *
     * @param method The method for which to get the generic type information.
     * @return The generic type information for the method, or {@code null} if the method is {@code null}.
     */
    public GenericMethod get(final Method method) {
        return method == null ? null : (GenericMethod) executableGenerics.computeIfAbsent(method, key -> compute(method));
    }

    /**
     * Computes the generic parameter types for an executable (method or constructor).
     * This method analyzes the parameters of the executable to determine their generic types.
     *
     * @param executable    The executable (method or constructor) whose parameters are to be analyzed.
     * @param declaringType The generic type information of the class declaring the executable.
     * @param consumer      A consumer that accepts a map of generic variable names to their corresponding parameter indices.
     *                      This map is used to resolve generic variable references within the executable's parameters.
     * @return An array of {@code Generic} objects representing the generic types of the executable's parameters.
     */
    protected Generic[] computeParameters(final Executable executable, final Generic declaringType,
                                          final Consumer<Map<String, Integer>> consumer) {
        Parameter[] parameters = executable.getParameters();
        Generic[] parameterTypes = new Generic[parameters.length];

        // Map to hold the positions of generic variables within the parameters.
        Map<String, Integer> variableTypes = new HashMap<>(2);
        Parameter parameter;
        Generic parameterType;
        for (int i = 0; i < parameters.length; i++) {
            parameter = parameters[i];
            parameterType = compute(parameter.getParameterizedType(), parameter.getType(), declaringType);
            parameterTypes[i] = parameterType;
            // Check if the parameter is of a generic type.
            if (parameter.getType() == Class.class && parameterType.size() == 1) {
                variableTypes.put(parameterType.getVariables().get(0).getName(), i);
            }
        }
        if (!variableTypes.isEmpty()) {
            // If there are generic variables, accept them using the provided consumer.
            if (consumer != null) {
                consumer.accept(variableTypes);
            }
            // Replace placeholders in generic types with actual positions.
            for (Generic type : parameterTypes) {
                type.place(variableTypes);
            }
        }

        return parameterTypes;
    }

    /**
     * Computes the generic types for the exceptions declared to be thrown by an executable (method or constructor).
     * This method analyzes the declared exceptions of the executable to determine their generic types.
     *
     * @param executable    The executable (method or constructor) whose declared exceptions are to be analyzed.
     * @param declaringType The generic type information of the class declaring the executable.
     * @return An array of {@code Generic} objects representing the generic types of the exceptions declared by the executable.
     */
    protected Generic[] computeExceptions(final Executable executable, final Generic declaringType) {
        Type[] genericExceptionTypes = executable.getGenericExceptionTypes();
        Class<?>[] exceptionTypes = executable.getExceptionTypes();
        Generic[] exceptionGenerics = new Generic[genericExceptionTypes.length];

        Generic exceptionGeneric;
        for (int i = 0; i < genericExceptionTypes.length; i++) {
            exceptionGeneric = compute(genericExceptionTypes[i], exceptionTypes[i], declaringType);
            exceptionGenerics[i] = exceptionGeneric;
        }

        return exceptionGenerics;
    }

    /**
     * Computes the generic type information for a method.
     * This includes the generic types of the method's return type, parameters, and exceptions.
     *
     * @param method The method for which to compute the generic type information.
     * @return A {@code GenericMethod} object encapsulating the generic type information of the method.
     */
    protected GenericMethod compute(final Method method) {
        // Retrieve the generic type information of the class declaring the method.
        Generic declaringType = typeGenerics.get(method.getDeclaringClass());

        // Compute the generic return type of the method.
        Generic returnType = compute(method.getGenericReturnType(), method.getReturnType(), declaringType);

        // Compute the generic types of the exceptions declared by the method.
        Generic[] exceptionsTypes = computeExceptions(method, declaringType);

        // Compute the generic types of the method's parameters.
        Generic[] parameterTypes = computeParameters(method, declaringType, variablePositions -> {
            // Place the generic type variables in the return type and exceptions based on their positions in parameters.
            returnType.place(variablePositions);
            for (Generic exceptionsType : exceptionsTypes) {
                exceptionsType.place(variablePositions);
            }
        });

        // Return a new GenericMethod object encapsulating the computed generic types.
        return new GenericMethod(method, parameterTypes, exceptionsTypes, returnType);
    }

    /**
     * Computes the generic type information for a given field.
     * This method determines the generic type of the field, taking into account the generic type information of the class that declares the field.
     *
     * @param field The field for which to compute the generic type information.
     * @return A {@code Generic} object representing the generic type information of the field.
     */
    protected Generic compute(final Field field) {
        // Compute the generic type of the field based on its declared generic type,
        // the actual type of the field, and the generic type information of the declaring class.
        return compute(field.getGenericType(), field.getType(), typeGenerics.get(field.getDeclaringClass()));
    }

    /**
     * Computes the generic type information for a constructor.
     * This includes determining the generic types of the constructor's parameters and exceptions.
     *
     * @param constructor The constructor for which to compute the generic type information.
     * @return A {@code GenericConstructor} object encapsulating the generic type information of the constructor.
     */
    protected GenericConstructor compute(final Constructor<?> constructor) {
        // Retrieve the generic type information of the class declaring the constructor.
        Generic declaringType = typeGenerics.get(constructor.getDeclaringClass());

        // Compute the generic types of the constructor's parameters.
        Generic[] parameterTypes = computeParameters(constructor, declaringType, null);

        // Compute the generic types of the exceptions declared by the constructor.
        Generic[] exceptionsTypes = computeExceptions(constructor, declaringType);

        // Return a new GenericConstructor object encapsulating the computed generic types.
        return new GenericConstructor(constructor, parameterTypes, exceptionsTypes);
    }

    /**
     * Computes generic type information based on a given type, its associated class, and the generic type information
     * of the class that declares it.
     *
     * @param type          The type to be analyzed for generic information.
     * @param cls           The class associated with the type.
     * @param declaringType The generic type information of the class that declares this type.
     * @return A {@code Generic} object representing the computed generic type information.
     */
    protected Generic compute(final Type type, final Class<?> cls, final Generic declaringType) {
        // Create a new Generic object based on the given type and its associated class.
        Generic generic = new Generic(type, cls);

        // Compute the generic type information, taking into account the declaring type's generics.
        // This involves potentially resolving type variables based on the declaring type's context.
        // The method `compute(Generic generic, Type type, Generic declaringType)` needs to be defined to handle this.
        generic.setType(compute(generic, type, declaringType));

        // Return the newly computed Generic object with updated type information.
        return generic;
    }

    /**
     * Computes the generic type, returning the resolved type based on the context of the declaring type.
     *
     * @param generic       The generic context in which the type resolution takes place.
     * @param type          The type to resolve.
     * @param declaringType The generic type information of the subclass that declares the type.
     * @return The resolved type.
     */
    protected Type compute(final Generic generic, final Type type, final Generic declaringType) {
        String name;
        if (type instanceof Class) {
            // No generic information.
        } else if (type instanceof TypeVariable) {
            // Variable
            TypeVariable<?> typeVariable = (TypeVariable<?>) type;
            name = typeVariable.getName();
            // Where the generic is declared
            GenericDeclaration gd = typeVariable.getGenericDeclaration();
            if (gd instanceof Class) {
                // Class variable
                if (declaringType != null) {
                    GenericVariable variable = declaringType.getVariable(name);
                    if (variable != null) {
                        generic.addVariable(variable);
                        if (variable.getType() != type) {
                            // Re-wrap the resolved variable to generate a Type
                            return variable.getType();
                        }
                    }
                }
            } else if (gd instanceof Executable) {
                // Executable variable (method & constructor)
                Type[] oldBounds = typeVariable.getBounds();
                // and compute the bounds of the variable
                Type[] newBounds = compute(generic, oldBounds, declaringType);
                typeVariable = oldBounds == newBounds ? typeVariable : new JTypeVariable<>(typeVariable, newBounds);
                generic.addVariable(new GenericVariable(name, typeVariable));
                if (typeVariable != type) {
                    return typeVariable;
                }
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] oldTypes = pType.getActualTypeArguments();
            Type[] newTypes = compute(generic, oldTypes, declaringType);
            if (newTypes != oldTypes) {
                // Re-wrap the resolved variable to generate a Type
                return new JParameterizedType(newTypes, pType.getOwnerType(), pType.getRawType());
            }
        } else if (type instanceof GenericArrayType) {
            // Generic array
            Type oldComponentType = ((GenericArrayType) type).getGenericComponentType();
            Type newComponentType = compute(generic, oldComponentType, declaringType);
            if (newComponentType != oldComponentType) {
                // Re-wrap the resolved variable to generate a Type
                return new JGenericArrayType(newComponentType);
            }
        } else if (type instanceof WildcardType) {
            // Wildcard
            WildcardType wildcardType = (WildcardType) type;
            Type[] oldUpperBounds = wildcardType.getUpperBounds();
            Type[] oldLowerBounds = wildcardType.getLowerBounds();
            Type[] newUpperBounds = compute(generic, oldUpperBounds, declaringType);
            Type[] newLowerBounds = compute(generic, oldLowerBounds, declaringType);
            if (oldUpperBounds != newUpperBounds || oldLowerBounds != newLowerBounds) {
                return new JWildcardType(newUpperBounds, newLowerBounds);
            }
        }
        return type;
    }


    /**
     * Computes and resolves an array of types based on the provided generic type information and the declaring type's context.
     * This method is typically used to resolve the types of parameters, return types, or exception types that may have generic parameters.
     *
     * @param genericType   The generic type information to use as a reference for resolution.
     * @param types         The array of types to resolve.
     * @param declaringType The generic type information of the class that declares these types.
     * @return An array of resolved types.
     */
    protected Type[] compute(final Generic genericType, final Type[] types, final Generic declaringType) {
        Type[] newTypes = new Type[types.length];
        boolean flag = false;
        for (int i = 0; i < types.length; i++) {
            // Resolve each generic parameter.
            newTypes[i] = compute(genericType, types[i], declaringType);
            // Check if the type has been changed during the resolution process.
            if (newTypes[i] != types[i]) {
                flag = true;
            }
        }
        // If any type has been changed, return the new array; otherwise, return the original.
        return flag ? newTypes : types;
    }

}
