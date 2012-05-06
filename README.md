WeiboNotifierForJenkins
=======================

A notifier for jenkins that publish build results to weibo.com

## How to install

1. Install [Apache Maven](http://maven.apache.org/download.html "Apache Maven Project")
2. [Configure maven for jenkins](https://wiki.jenkins-ci.org/display/JENKINS/Plugin+tutorial#Plugintutorial-SettingUpEnvironment "Setting up environment")
3. Run `mvn install`
4. Copy `target/weibo-notifier.hpi` to `%JENKINS_HOME%/plugins`
5. Restart jenkins
6. Enjoy!