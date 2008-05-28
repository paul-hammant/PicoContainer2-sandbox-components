/*****************************************************************************
 * Copyright (C) PicoContainer Organization. All rights reserved.            *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Michael Rimov                                            *
 *****************************************************************************/


package org.picocontainer.script.groovy.buildernodes;

import java.util.Map;

import org.picocontainer.PicoContainer;
import org.picocontainer.script.ScriptedPicoContainerMarkupException;
import org.picocontainer.script.ScriptedPicoContainer;

/**
 * Sometimes it is worthwhile to split apart Nanocontainer building
 * into functions.  For example, you might want to group adding the domain
 * object repositories (DAOs) into a single function to make your composition script
 * easier to maintain.
 * <p>Unfortunately, normally this is not allowed under normal builder rules.  If
 * you wish to separate code you must revert to standard picocontainer calling
 * systax.</p>
 * <p>This node corrects that deficiency.</p>
 * <p>With it you can perform:
 * <code><pre>
 * pico = builder.container(parent:parent) {
 * &nbsp;&nbsp;component(....)
 * &nbsp;&nbsp;//...
 * }
 * <br/>
 * //<em>Now add more to pico.</em>
 * builder.append(container: pico) {
 * &nbsp;&nbsp;component(....)
 * &nbsp;&nbsp;//...
 * }
 * </pre></code>
 * </p>
 * @author Michael Rimov
 */
public class AppendContainerNode extends AbstractBuilderNode {
    /**
     * Node name.
     */
    public static final String NODE_NAME = "append";


    /**
     * Supported Attribute (Required): 'container.'  Reference to the container
     * we are going to append to.
     */
    public static final String CONTAINER = "container";

    /**
     * Constructs an append container node.
     */
    public AppendContainerNode() {
        super(NODE_NAME);
    }

    /**
     * Returns the container passed in as the &quot;container&quot; attribute.
     * @param current Object unused.
     * @param attributes Map attributes passed in.  This must have the container
     * attribute defined.
     * @return Object the passed in Nanocontainer.
     * @throws ScriptedPicoContainerMarkupException if the container attribute
     * is not supplied.
     * @throws ClassCastException if the container node specified is not a  ScriptedPicoContainer or PicoContainer
     */
    public Object createNewNode(final Object current, final Map attributes) throws ScriptedPicoContainerMarkupException, ClassCastException {
        if (!isAttribute(attributes, CONTAINER)) {
            throw new ScriptedPicoContainerMarkupException(NODE_NAME + " must have a container attribute");
        }


        Object attributeValue = attributes.get(CONTAINER);
        if (! (attributeValue instanceof ScriptedPicoContainer) && !(attributeValue instanceof PicoContainer) ) {
            throw new ClassCastException(attributeValue.toString() + " must be a derivative of ScriptedPicoContainer or PicoContainer.  Got: "
                + attributeValue.getClass().getName() + " instead.");
        }
        return attributeValue;
    }


}
