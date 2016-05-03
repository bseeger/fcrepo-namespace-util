#Fedora Namespace Utility

This utility reads the registered namespaces for a Fedora repository and allows an administrator to selectively modify the namespace prefixes.

## Running

Before running this namespace utility, stop the repository by shutting down the servlet container (Tomcat, Jetty, etc.). The utility requires the `fcrepo.home` system property.

Next, run this utility with the following command:

``` sh
java -Dfcrepo.home=/path/to/data -jar path/to/fcrepo-namespace-util.jar
```

If your repository uses a customized `repository.json` configuration, you will need to specify the
location of that resource such as:

``` sh
java -Dfcrepo.home=/path/to/data -Dfcrepo.modeshape.configuration=file:/etc/fcrepo/repository.json -jar path/to/fcrepo-namespace-util.jar
```

