/*****************************************************************************
 * Copyright (C) PicoContainer Organization. All rights reserved.            *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Paul Hammant & Obie Fernandez & Aslak                    *
 *****************************************************************************/

package org.picocontainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * A component monitor is responsible for monitoring the component instantiation
 * and method invocation.
 * 
 * @author Paul Hammant
 * @author Obie Fernandez
 * @author Aslak Helles&oslash;y
 * @author Mauro Talevi
 * @version $Revision$
 * @since 1.2
 */
public interface ComponentMonitor {

    /**
     * Event thrown as the component is being instantiated using the given constructor
     * 
     * @param componentAdapter
     * @param constructor the Constructor used to instantiate the addComponent
     * @return the constructor to use in instantiation (nearly always the same one as passed in)
     */
    Constructor instantiating(ComponentAdapter componentAdapter,
                              Constructor constructor
    );

    /**
     * Event thrown after the component has been instantiated using the given constructor.
     * This should be called for both Constructor and Setter DI.
     *
     * @param componentAdapter
     *@param constructor the Constructor used to instantiate the addComponent
     * @param instantiated the component that was instantiated by PicoContainer
     * @param injected the components during instantiation.
     * @param duration the duration in millis of the instantiation @since 1.3
     */

    void instantiated(ComponentAdapter componentAdapter,
                      Constructor constructor,
                      Object instantiated,
                      Object[] injected,
                      long duration);

    /**
     * Event thrown if the component instantiation failed using the given constructor
     * 
     * @param componentAdapter
     * @param constructor the Constructor used to instantiate the addComponent
     * @param cause the Exception detailing the cause of the failure
     */
    void instantiationFailed(ComponentAdapter componentAdapter, Constructor constructor, Exception cause);

    /**
     * Event thrown as the component method is being invoked on the given instance
     * 
     * @param method the Method invoked on the component instance
     * @param instance the component instance
     */
    void invoking(Method method, Object instance);

    /**
     * Event thrown after the component method has been invoked on the given instance
     * 
     * @param method the Method invoked on the component instance
     * @param instance the component instance
     * @param duration the duration in millis of the invocation
     */
    void invoked(Method method, Object instance, long duration);

    /**
     * Event thrown if the component method invocation failed on the given instance
     * 
     * @param method the Method invoked on the component instance
     * @param instance the component instance
     * @param cause the Exception detailing the cause of the failure
     */
    void invocationFailed(Method method, Object instance, Exception cause);

    /**
     * Event thrown if a lifecycle method invocation - start, stop or dispose - 
     * failed on the given instance
     *
     * @param method the lifecycle Method invoked on the component instance
     * @param instance the component instance
     * @param cause the RuntimeException detailing the cause of the failure
     */
    void lifecycleInvocationFailed(Method method, Object instance, RuntimeException cause);


    void noComponent(Object componentKey);
}
