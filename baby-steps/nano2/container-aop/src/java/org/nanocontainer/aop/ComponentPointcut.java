/*****************************************************************************
 * Copyright (c) PicoContainer Organization. All rights reserved.            *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Idea by Rachel Davies, Original code by various                           *
 *****************************************************************************/
package org.nanocontainer.aop;

/**
 * Pointcut that picks addComponent keys.
 *
 * @author Stephen Molitor
 * @version $Revision$
 */
public interface ComponentPointcut {

    /**
     * Returns true if the addComponent key satisfies this pointcut.
     *
     * @param componentKey the addComponent key.
     * @return true if the pointcut is satisfied, else false.
     */
    boolean picks(Object componentKey);

}
