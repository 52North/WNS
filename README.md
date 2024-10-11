# ARCHIVED

This project is no longer maintained and will not receive any further updates. If you plan to continue using it, please be aware that future security issues will not be addressed.

Documentation can be found online under 

  <https://wiki.52north.org/bin/view/SensorWeb/WebNotificationService>
  
Later version will include an update on the documentation.

To build the WNS, use Maven project management tool.
Ensure you have set predefined build variables within `pom.xml` or use the `env-dev` profile along with properties file `wns_v2-build-dev.properties` on your `${user.home}`.
Then build with `mvn -P env-dev`.
