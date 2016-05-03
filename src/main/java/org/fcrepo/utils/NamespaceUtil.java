package org.fcrepo.utils;

import static java.util.Arrays.stream;
import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.kernel.modeshape.utils.NamespaceTools.getNamespaces;

import java.io.File;
import java.util.Map;
import java.util.Scanner;

import javax.inject.Inject;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.NamespaceException;

import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.NodeService;

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
     * Migrate technical metadata.
     * @param args If "dryrun" is passed as an argument, the utility will print out what would be done,
     *             but no changes will be made.
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
        prompt(sessionFactory.getInternalSession());
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
                System.out.println("Enter a new prefix for the URI (or 'ctrl-d' to cancel): " + namespaces.get(prefix));
                if (scan.hasNextLine()) {
                    final String newPrefix = scan.nextLine();
                    try {
                        session.getWorkspace().getNamespaceRegistry().registerNamespace(newPrefix, namespaces.get(prefix));
                        session.save();
                    } catch (final NamespaceException ex) {
                        System.out.println("Could not change prefix (" + prefix + "): " + ex.getMessage());
                    }
                }
            } else {
                System.out.println("Invalid prefix: " + prefix);
            }
            prompt(session);
        } else {
            System.out.println("Goodbye");
        }
    }
}

