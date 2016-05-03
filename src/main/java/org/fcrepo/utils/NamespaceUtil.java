/*
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.utils;

import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.kernel.modeshape.utils.NamespaceTools.getNamespaces;

import java.util.Map;
import java.util.Scanner;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.NamespaceException;

import org.fcrepo.http.commons.session.SessionFactory;

import org.slf4j.Logger;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * Utility to manipulate namespace prefixes
 *
 * @author acoburn
 * @since May 3, 2016
 **/
public class NamespaceUtil {

    private final Logger LOGGER = getLogger(NamespaceUtil.class);

    @Inject
    private SessionFactory sessionFactory;

    /**
     * Start and run the namespace utility
     **/
    public static void main(final String[] args) {
        ConfigurableApplicationContext ctx = null;
        try {
            final NamespaceUtil nsUtil = new NamespaceUtil();
            ctx = new ClassPathXmlApplicationContext("classpath:/spring/master.xml");
            ctx.getBeanFactory().autowireBeanProperties(nsUtil, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
            nsUtil.run();
        } catch (RepositoryException ex) {
            ex.printStackTrace();
        } finally {
            if (null != ctx) {
                ctx.close();
            }
        }
    }

    /**
     * Run the namespace change utility
     **/
    public void run() throws RepositoryException {
        LOGGER.info("Starting namespace utility");
        prompt(sessionFactory.getInternalSession());
        LOGGER.info("Stopping namespace utility");
    }

    private void prompt(final Session session) throws RepositoryException {
        final Map<String, String> namespaces = getNamespaces(session);

        System.out.println();
        System.out.println("#####################################");
        namespaces.forEach((k, v) -> System.out.println("Prefix " + k + ": " + v));
        System.out.println("#####################################");
        System.out.println();
        System.out.println("Enter a prefix to change (or 'ctrl-d' to end):");

        final Scanner scan = new Scanner(System.in);
        if (scan.hasNextLine()) {
            final String prefix = scan.nextLine();
            if (namespaces.containsKey(prefix)) {
                System.out.println("Enter a new prefix for the URI (or 'ctrl-d' to cancel): " +
                        namespaces.get(prefix));
                if (scan.hasNextLine()) {
                    final String newPrefix = scan.nextLine();
                    try {
                        session.getWorkspace().getNamespaceRegistry().registerNamespace(newPrefix,
                                namespaces.get(prefix));
                        session.save();
                    } catch (final NamespaceException ex) {
                        System.out.println("Could not change prefix (" + prefix + "): " + ex.getMessage());
                    }
                }
            } else {
                System.out.println("Invalid prefix: " + prefix);
            }
            prompt(session);
        }
    }
}

