/*****************************************************************************
 * Copyright (C) NanoContainer Organization. All rights reserved.            *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/

package org.nanocontainer.script;

import org.picocontainer.Parameter;
import org.nanocontainer.ClassName;
import org.nanocontainer.NanoContainer;

public class ComponentElementHelper {

    public static Object makeComponent(Object cnkey, Object key, Parameter[] parameters, Object klass, NanoContainer current, Object instance) {
        if (cnkey != null)  {
            key = new ClassName((String)cnkey);
        }

        if (klass instanceof Class) {
            Class clazz = (Class) klass;
            key = key == null ? clazz : key;
            return current.addComponent(key, clazz, parameters);
        } else if (klass instanceof String) {
            String className = (String) klass;
            key = key == null ? className : key;
            return current.addComponent(key, new ClassName(className), parameters);
        } else if (instance != null) {
            key = key == null ? instance.getClass() : key;
            return current.addComponent(key, instance);
        } else {
            throw new NanoContainerMarkupException("Must specify a 'class' attribute for a addComponent as a class name (string) or Class.");
        }
    }


}
