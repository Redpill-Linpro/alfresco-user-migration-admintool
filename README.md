Alfresco User Migration Tool
=============================================

This module is sponsored by Redpill Linpro AB - http://www.redpill-linpro.com.

Description
-----------
This Share Admin console tool is used when there is a need to change the username of one or more Alfresco users. It transfers node ownerships, site and group memberships from one user to another. The original user stays but could be marked as disabled by preference.

Building & Installation
------------
The build produces two amp-files and two jar files, two for the repository and two for share. Use the module management tool to install the amps onto the war files or include dependencies to the jar files to get the functionality into your custom amp.

Repository dependency:
```xml
<dependency>
  <groupId>org.redpill-linpro.alfresco</groupId>
  <artifactId>alfresco-user-migration-admintool-repo</artifactId>
  <version>1.1.0</version>
</dependency>
```

Share dependency:
```xml
<dependency>
  <groupId>org.redpill-linpro.alfresco</groupId>
  <artifactId>alfresco-user-migration-admintool-share</artifactId>
  <version>1.1.0</version>
</dependency>
```

Maven repository:
```xml
<repository>
  <id>redpill-public</id>
  <url>http://maven.redpill-linpro.com/nexus/content/groups/public</url>
</repository>
```

The amp/jar files are also downloadable from: https://maven.redpill-linpro.com/nexus/index.html#nexus-search;quick~alfresco-user-migration-admintool
Usage
------------
Log in as admin and a new admin console tool "Replace User" will materialize with instructions.

License
-------

This application is licensed under the LGPLv3 License. See the [LICENSE file](LICENSE) for details.

Authors
-------

Marcus Svensson - Redpill Linpro AB
Erik Billerby - Redpill Linpro AB
