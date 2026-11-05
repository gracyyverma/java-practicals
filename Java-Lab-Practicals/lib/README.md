# lib

Third-party jars used by **Lab 09** and **Lab 10**. They were downloaded from Maven Central and checked against the SHA-1 checksums published there.

| Jar | Version | Used for | License |
|:--|:--|:--|:--|
| `mysql-connector-j-8.4.0.jar` | 8.4.0 | MySQL JDBC driver (Type 4, pure Java) | GPL-2.0 with Universal FOSS Exception |
| `HikariCP-5.1.0.jar` | 5.1.0 | connection pool (Lab 10) | Apache-2.0 |
| `slf4j-api-2.0.13.jar` | 2.0.13 | logging API required by HikariCP | MIT |
| `slf4j-simple-2.0.13.jar` | 2.0.13 | prints HikariCP's pool start / shutdown messages | MIT |

Use them on the classpath from a lab folder:

```bash
java -cp "..\lib\*" Lab09.java          # Windows
java -cp "../lib/*" Lab09.java          # Linux / macOS
```
