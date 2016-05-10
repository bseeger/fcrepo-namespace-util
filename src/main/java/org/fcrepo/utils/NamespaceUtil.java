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
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.NamespaceException;
//import javax.jcr.NamespaceRegistry;

import org.fcrepo.http.commons.session.SessionFactory;

import org.yaml.snakeyaml.Yaml;

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

    @Inject
    private Yaml yaml;

    /**
     * Start and run the namespace utility
     **/
    public static void main(final String[] args) {
        ConfigurableApplicationContext ctx = null;
        try {
            final NamespaceUtil nsUtil = new NamespaceUtil();
            ctx = new ClassPathXmlApplicationContext("classpath:/spring/master.xml");
            ctx.getBeanFactory().autowireBeanProperties(nsUtil, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
            nsUtil.run(args.length > 0 ? args[0] : null);
        } catch (RepositoryException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            System.out.println("Prefix file not found.");
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
    public void run(final String prefixFilename) throws RepositoryException,
                                                     FileNotFoundException {
        LOGGER.info("Starting namespace utility");
        final Session session = sessionFactory.getInternalSession();
        if (prefixFilename != null) {
            LOGGER.info("Importing prefixes from file: {}", prefixFilename);
            loadPrefixFile(session, prefixFilename);
        }
        prompt(session);
        LOGGER.info("Stopping namespace utility");
    }

    private Boolean updatePrefix(final Session session, final Map<String,String> namespaces, final String prefix,
                                 final String uri) {

        LOGGER.info("Adding prefix {}: {}",prefix, uri);

        try {
            session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uri);
            session.save();

            LOGGER.info("Prefix {} successfully update", prefix);
            return true;
        } catch (final NamespaceException ex) {
            LOGGER.info("NamespaceException occured while updating prefix {}: {}", prefix,
                ex.getMessage());
            return false;
        } catch (final RepositoryException ex) {
            LOGGER.info("RepositoryException occured while updating prefix {}: {}", prefix,
                ex.getMessage());
            return false;
        }
    }

    private void loadPrefixFile(final Session session, final String prefixFile) throws RepositoryException,
                                                                    FileNotFoundException  {

        final Map<String,String> prefixes = (Map<String,String>)yaml.load(new FileInputStream(prefixFile));
        final Map<String, String> namespaces = getNamespaces(session);

        System.out.println("\n\n#################### Importing Prefixes #########################");
        prefixes.forEach((prefix, v) -> {
            String value = v;
            Boolean replace = false;
            final Boolean exists = namespaces.containsKey(prefix);
            if (exists) {
                value = namespaces.get(prefix);

                System.out.println("System already contains URI \"" + namespaces.get(prefix) +
                                   "\" with prefix \"" + prefix + "\"");
                System.out.println("Do you want to replace the prefix with \"" + prefix + "\"? (Y/n): ");

            } else {
              System.out.println("Are you sure you want to add prefix \"" + prefix + "\" with value: \"" +
                                 value + "\"? (Y/n): ");
            }

            final Scanner scanIn = new Scanner(System.in);
            final String response = scanIn.nextLine().trim();

            // anything other then a Y/y or blank will result in the changes NOT happening.
            if (response.trim().toLowerCase().equals("y") || response.trim().toLowerCase().isEmpty()) {
                replace = true;
            }

            if (replace) {
               if (updatePrefix(session, namespaces, prefix, value)) {
                  System.out.println("Prefix \"" + prefix + "\" (" + value + ") " +
                      (exists ? "edited" : "added") + " successfully.");
               } else {
                  System.out.println("Failed to edit \"" + prefix + "\": <" + value + ">.");
               }
            } else {
                System.out.println("Prefix \"" + prefix + "\" skipped.");
            }
            System.out.println();
        });
    }


    private void prompt(final Session session) throws RepositoryException {
        final Map<String, String> namespaces = getNamespaces(session);

        System.out.println();
        System.out.println("############### Current Fedora Prefix List ######################");
        namespaces.forEach((k, v) -> System.out.println("Prefix " + k + ": " + v));
        System.out.println("#################################################################");
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
                    if (!updatePrefix(session, namespaces, newPrefix, namespaces.get(prefix))) {
                        System.out.println("Could not change prefix (" + prefix + ")");
                    }
                }
            } else {
                System.out.println("Invalid prefix: " + prefix);
            }
            prompt(session);
        }
    }
}
