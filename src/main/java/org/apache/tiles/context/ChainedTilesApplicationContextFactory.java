/*
 * $Id: ChainedTilesApplicationContextFactory.java 774104 2009-05-12 21:44:39Z apetrelli $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.tiles.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.Initializable;
import org.apache.tiles.TilesApplicationContext;
import org.apache.tiles.awareness.AbstractTilesApplicationContextFactoryAware;
import org.apache.tiles.reflect.ClassUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implementation for TilesApplicationContextFactory, that creates a chain of
 * sub-factories, trying each one until it returns a not-null value.
 *
 * @version $Rev: 774104 $ $Date: 2009-05-12 23:44:39 +0200(mar, 12 mag 2009) $
 * @since 2.1.1
 */
public class ChainedTilesApplicationContextFactory extends
        AbstractTilesApplicationContextFactory implements Initializable {

    /**
     * Factory class names initialization parameter to use.
     *
     * @since 2.1.1
     */
    public static final String FACTORY_CLASS_NAMES =
        "org.apache.tiles.context.ChainedTilesApplicationContextFactory.FACTORY_CLASS_NAMES";

    /**
     * The default class names to instantiate that compose the chain..
     *
     * @since 2.1.1
     */
    public static final String[] DEFAULT_FACTORY_CLASS_NAMES = {
            "org.apache.tiles.servlet.context.ServletTilesApplicationContextFactory",
            "org.apache.tiles.portlet.context.PortletTilesApplicationContextFactory"};

    /**
     * The logging object.
     */
    private final Log log = LogFactory
            .getLog(ChainedTilesApplicationContextFactory.class);

    /**
     * The Tiles context factories composing the chain.
     */
    private List<AbstractTilesApplicationContextFactory> factories;

    /**
     * Sets the factories to be used.
     *
     * @param factories The factories to be used.
     */
    public void setFactories(
            List<AbstractTilesApplicationContextFactory> factories) {
        this.factories = factories;
    }

    /** {@inheritDoc} */
    public void init(Map<String, String> configParameters) {
        String[] classNames = null;
        String classNamesString = configParameters.get(FACTORY_CLASS_NAMES);
        if (classNamesString != null) {
            classNames = classNamesString.split("\\s*,\\s*");
        }
        if (classNames == null || classNames.length <= 0) {
            classNames = DEFAULT_FACTORY_CLASS_NAMES;
        }

        factories = new ArrayList<AbstractTilesApplicationContextFactory>();
        for (int i = 0; i < classNames.length; i++) {
            try {
                Class<? extends AbstractTilesApplicationContextFactory> clazz = ClassUtil
                        .getClass(classNames[i],
                                AbstractTilesApplicationContextFactory.class);
                AbstractTilesApplicationContextFactory factory = clazz
                        .newInstance();
                if (factory instanceof AbstractTilesApplicationContextFactoryAware) {
                    ((AbstractTilesApplicationContextFactoryAware) factory)
                            .setApplicationContextFactory(this);
                }
                factories.add(factory);
            } catch (ClassNotFoundException e) {
                // We log it, because it could be a default configuration class that
                // is simply not present.
                log.warn("Cannot find TilesContextFactory class "
                        + classNames[i]);
                if (log.isDebugEnabled()) {
                    log.debug("Cannot find TilesContextFactory class "
                            + classNames[i], e);
                }
            } catch (InstantiationException e) {
                throw new IllegalArgumentException(
                        "Cannot instantiate TilesFactoryClass " + classNames[i],
                        e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(
                        "Cannot access TilesFactoryClass " + classNames[i]
                                + " default constructor", e);
            }
        }
    }

    /** {@inheritDoc} */
    public TilesApplicationContext createApplicationContext(Object context) {
        TilesApplicationContext retValue = null;

        for (Iterator<AbstractTilesApplicationContextFactory> factoryIt = factories
                .iterator(); factoryIt.hasNext() && retValue == null;) {
            retValue = factoryIt.next().createApplicationContext(context);
        }

        if (retValue == null) {
            throw new IllegalArgumentException(
                    "Cannot find a factory to create the application context");
        }

        return retValue;
    }
}
