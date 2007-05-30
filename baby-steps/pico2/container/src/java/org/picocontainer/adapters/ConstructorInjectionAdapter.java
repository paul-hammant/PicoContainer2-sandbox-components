/*****************************************************************************
 * Copyright (c) PicoContainer Organization. All rights reserved.            *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Idea by Rachel Davies, Original code by Aslak Hellesoy and Paul Hammant   *
 *****************************************************************************/

package org.picocontainer.adapters;

import org.picocontainer.ComponentMonitor;
import org.picocontainer.Parameter;
import org.picocontainer.PicoContainer;
import org.picocontainer.PicoInitializationException;
import org.picocontainer.PicoIntrospectionException;
import org.picocontainer.ParameterName;
import org.picocontainer.adapters.InjectingAdapter;
import org.picocontainer.defaults.ThreadLocalCyclicDependencyGuard;
import org.picocontainer.LifecycleStrategy;
import org.picocontainer.defaults.AssignabilityRegistrationException;
import org.picocontainer.defaults.NotConcreteRegistrationException;
import org.picocontainer.defaults.UnsatisfiableDependenciesException;
import org.picocontainer.defaults.AmbiguousComponentResolutionException;
import org.picocontainer.defaults.PicoInvocationTargetInitializationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.thoughtworks.paranamer.Paranamer;
import com.thoughtworks.paranamer.asm.AsmParanamer;

/**
 * Instantiates components using Constructor Injection.
 * <em>
 * Note that this class doesn't cache instances. If you want caching,
 * use a {@link CachingBehaviorAdapter} around this one.
 * </em>
 *
 * @author Paul Hammant
 * @author Aslak Helles&oslash;y
 * @author Jon Tirs&eacute;n
 * @author Zohar Melamed
 * @author J&ouml;rg Schaible
 * @author Mauro Talevi
 * @version $Revision$
 */
public class ConstructorInjectionAdapter extends InjectingAdapter {
    private transient List<Constructor> sortedMatchingConstructors;
    private transient Guard instantiationGuard;
    private transient Paranamer paranamer = new AsmParanamer();

    private static abstract class Guard extends ThreadLocalCyclicDependencyGuard {
        protected PicoContainer guardedContainer;

        private void setArguments(PicoContainer container) {
            this.guardedContainer = container;
        }
    }

    /**
     * Creates a ConstructorInjectionComponentAdapter
     *
     * @param componentKey            the search key for this implementation
     * @param componentImplementation the concrete implementation
     * @param parameters              the parameters to use for the initialization
     * @param monitor                 the addComponent monitor used by this addAdapter
     * @param lifecycleStrategy       the addComponent lifecycle strategy used by this addAdapter
     * @throws org.picocontainer.defaults.AssignabilityRegistrationException
     *                              if the key is a type and the implementation cannot be assigned to.
     * @throws org.picocontainer.defaults.NotConcreteRegistrationException
     *                              if the implementation is not a concrete class.
     * @throws NullPointerException if one of the parameters is <code>null</code>
     */
    public ConstructorInjectionAdapter(final Object componentKey, final Class componentImplementation, Parameter[] parameters, ComponentMonitor monitor, LifecycleStrategy lifecycleStrategy) throws AssignabilityRegistrationException, NotConcreteRegistrationException {
        super(componentKey, componentImplementation, parameters, monitor, lifecycleStrategy);
    }

    /**
     * Creates a ConstructorInjectionComponentAdapter
     *
     * @param componentKey            the search key for this implementation
     * @param componentImplementation the concrete implementation
     * @param parameters              the parameters to use for the initialization
     * @param monitor                 the addComponent monitor used by this addAdapter
     * @throws AssignabilityRegistrationException
     *                              if the key is a type and the implementation cannot be assigned to.
     * @throws NotConcreteRegistrationException
     *                              if the implementation is not a concrete class.
     * @throws NullPointerException if one of the parameters is <code>null</code>
     */
    public ConstructorInjectionAdapter(final Object componentKey, final Class componentImplementation, Parameter[] parameters, ComponentMonitor monitor) throws AssignabilityRegistrationException, NotConcreteRegistrationException {
        super(componentKey, componentImplementation, parameters, monitor);
    }

    /**
     * Creates a ConstructorInjectionComponentAdapter
     *
     * @param componentKey            the search key for this implementation
     * @param componentImplementation the concrete implementation
     * @param parameters              the parameters to use for the initialization
     * @throws AssignabilityRegistrationException
     *                              if the key is a type and the implementation cannot be assigned to.
     * @throws NotConcreteRegistrationException
     *                              if the implementation is not a concrete class.
     * @throws NullPointerException if one of the parameters is <code>null</code>
     */
    public ConstructorInjectionAdapter(final Object componentKey, final Class componentImplementation, Parameter... parameters) throws AssignabilityRegistrationException, NotConcreteRegistrationException {
        super(componentKey, componentImplementation, parameters);
    }

    /**
     * Creates a ConstructorInjectionComponentAdapter with key and implementation
     *
     * @param componentKey            the search key for this implementation
     * @param componentImplementation the concrete implementation
     * @throws AssignabilityRegistrationException
     *                              if the key is a type and the implementation cannot be assigned to.
     * @throws NotConcreteRegistrationException
     *                              if the implementation is not a concrete class.
     * @throws NullPointerException if one of the parameters is <code>null</code>
     */
    public ConstructorInjectionAdapter(Object componentKey, Class componentImplementation) throws AssignabilityRegistrationException, NotConcreteRegistrationException {
        this(componentKey, componentImplementation, (Parameter[])null);
    }

    protected Constructor getGreediestSatisfiableConstructor(PicoContainer container) throws PicoIntrospectionException, AssignabilityRegistrationException, NotConcreteRegistrationException {
        final Set<Constructor> conflicts = new HashSet<Constructor>();
        final Set<List<Class>> unsatisfiableDependencyTypes = new HashSet<List<Class>>();
        if (sortedMatchingConstructors == null) {
            sortedMatchingConstructors = getSortedMatchingConstructors();
        }
        Constructor greediestConstructor = null;
        int lastSatisfiableConstructorSize = -1;
        Class unsatisfiedDependencyType = null;
        for (Constructor sortedMatchingConstructor : sortedMatchingConstructors) {
            boolean failedDependency = false;
            final Constructor constructor = sortedMatchingConstructor;
            Class[] parameterTypes = constructor.getParameterTypes();
            Parameter[] currentParameters = parameters != null ? parameters : createDefaultParameters(parameterTypes);

            // remember: all constructors with less arguments than the given parameters are filtered out already
            for (int j = 0; j < currentParameters.length; j++) {
                // check wether this constructor is statisfiable
                final int j1 = j;
                if (currentParameters[j].isResolvable(container, this, parameterTypes[j], new ParameterName() {
                    public String getParameterName() {
                        String[] names = paranamer.lookupParameterNames(constructor);
                        if (names.length != 0) {
                            return names[j1];
                        }
                        return null;
                    }
                })) {
                    continue;
                }
                unsatisfiableDependencyTypes.add(Arrays.asList(parameterTypes));
                unsatisfiedDependencyType = parameterTypes[j];
                failedDependency = true;
                break;
            }

            if (greediestConstructor != null && parameterTypes.length != lastSatisfiableConstructorSize) {
                if (conflicts.isEmpty()) {
                    // we found our match [aka. greedy and satisfied]
                    return greediestConstructor;
                } else {
                    // fits although not greedy
                    conflicts.add(constructor);
                }
            } else if (!failedDependency && lastSatisfiableConstructorSize == parameterTypes.length) {
                // satisfied and same size as previous one?
                conflicts.add(constructor);
                conflicts.add(greediestConstructor);
            } else if (!failedDependency) {
                greediestConstructor = constructor;
                lastSatisfiableConstructorSize = parameterTypes.length;
            }
        }
        if (!conflicts.isEmpty()) {
            throw new PicoIntrospectionException(conflicts.size() + " satisfiable constructos is too many for '"+getComponentImplementation()+"'. Constructor List:" + conflicts.toString().replace(getComponentImplementation().getName(),"<init>").replace("public <i","<i"));
        } else if (greediestConstructor == null && !unsatisfiableDependencyTypes.isEmpty()) {
            throw new UnsatisfiableDependenciesException(this, unsatisfiedDependencyType, unsatisfiableDependencyTypes, container);
        } else if (greediestConstructor == null) {
            // be nice to the user, show all constructors that were filtered out
            final Set<Constructor> nonMatching = new HashSet<Constructor>();
            for (Constructor constructor : getConstructors()) {
                nonMatching.add(constructor);
            }
            throw new PicoInitializationException("Either the specified parameters do not match any of the following constructors: " + nonMatching.toString() + "; OR the constructors were not accessible for '" + getComponentImplementation().getName() + "'");
        }
        return greediestConstructor;
    }



    public Object getComponentInstance(PicoContainer container) throws PicoInitializationException, PicoIntrospectionException, AssignabilityRegistrationException, NotConcreteRegistrationException {
        if (instantiationGuard == null) {
            instantiationGuard = new Guard() {
                public Object run() {
                    final Constructor constructor;
                    try {
                        constructor = getGreediestSatisfiableConstructor(guardedContainer);
                    } catch (AmbiguousComponentResolutionException e) {
                        e.setComponent(getComponentImplementation());
                        throw e;
                    }
                    ComponentMonitor componentMonitor = currentMonitor();
                    try {
                        Object[] parameters = getConstructorArguments(guardedContainer, constructor);
                        componentMonitor.instantiating(constructor);
                        long startTime = System.currentTimeMillis();
                        Object inst = newInstance(constructor, parameters);
                        componentMonitor.instantiated(constructor, inst, parameters, System.currentTimeMillis() - startTime);
                        return inst;
                    } catch (InvocationTargetException e) {
                        componentMonitor.instantiationFailed(constructor, e);
                        if (e.getTargetException() instanceof RuntimeException) {
                            throw (RuntimeException) e.getTargetException();
                        } else if (e.getTargetException() instanceof Error) {
                            throw (Error) e.getTargetException();
                        }
                        throw new PicoInvocationTargetInitializationException(e.getTargetException());
                    } catch (InstantiationException e) {
                        // can't get here because checkConcrete() will catch it earlier, but see PICO-191
                        ///CLOVER:OFF
                        componentMonitor.instantiationFailed(constructor, e);
                        throw new PicoInitializationException("Should never get here");
                        ///CLOVER:ON
                    } catch (IllegalAccessException e) {
                        // can't get here because either filtered or access mode set
                        ///CLOVER:OFF
                        componentMonitor.instantiationFailed(constructor, e);
                        throw new PicoInitializationException(e);
                        ///CLOVER:ON
                    }
                }
            };
        }
        instantiationGuard.setArguments(container);
        return instantiationGuard.observe(getComponentImplementation());
    }

    protected Object[] getConstructorArguments(PicoContainer container, final Constructor ctor) {
        Class[] parameterTypes = ctor.getParameterTypes();
        Object[] result = new Object[parameterTypes.length];
        Parameter[] currentParameters = parameters != null ? parameters : createDefaultParameters(parameterTypes);

        for (int i = 0; i < currentParameters.length; i++) {
            final int i1 = i;
            result[i] = currentParameters[i].resolveInstance(container, this, parameterTypes[i], new ParameterName() {
                public String getParameterName() {
                    String[] strings = paranamer.lookupParameterNames(ctor);
                    return strings.length == 0 ? "" : strings[i1];
                }
            });
        }
        return result;
    }

    private List<Constructor> getSortedMatchingConstructors() {
        List<Constructor> matchingConstructors = new ArrayList<Constructor>();
        Constructor[] allConstructors = getConstructors();
        // filter out all constructors that will definately not match
        for (Constructor constructor : allConstructors) {
            if ((parameters == null || constructor.getParameterTypes().length == parameters.length) && (constructor.getModifiers() & Modifier.PUBLIC) != 0) {
                matchingConstructors.add(constructor);
            }
        }
        // optimize list of constructors moving the longest at the beginning
        if (parameters == null) {
            Collections.sort(matchingConstructors, new Comparator() {
                public int compare(Object arg0, Object arg1) {
                    return ((Constructor) arg1).getParameterTypes().length - ((Constructor) arg0).getParameterTypes().length;
                }
            });
        }
        return matchingConstructors;
    }

    private Constructor[] getConstructors() {
        return (Constructor[]) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return getComponentImplementation().getDeclaredConstructors();
            }
        });
    }
}
