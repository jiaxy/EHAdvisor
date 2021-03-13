# README

This is the replication package of the paper **"Where to Handle an Exception? Recommending Exception Handling Locations from a Global Perspective"**.

## Content

This package includes the codes to replicate the experiments in the paper as following:

* `Graph`: This folder includes the codes to use AST Parser JDT to parse the source codes in a project and construct the numerical samples that are represented with the four features (i.e. architectural feature, project feature, functional feature, and exception feature). The entry is `App.java`. You can use it to construct samples for any other Java project.
* `Dataset`: For the convenience to replicate, we release the extracted samples from the 29 popular Java projects. This folder includes the data we adopt in the experiments.
* `Train`: This folder includes the scripts to train the binary classification model with AutoGluon.
* `FeatImportanceAnalysis`: This folder includes the scripts to calculate the importance score of each feature with AutoGluon.

## Replication Instruction

### Package Requirement

* Python 3.8
* AutoGluon 0.15

For more details about the package requiment, we provide a package list in `requirements.txt`. You can type the command `pip install -r requirements.txt` to easily construct the experiment environment.

### Exepriment Instruction:

* Train and apply the classification model with a specific scenario (i.e. one of across-project, intra-project, and intra-project+) as following, where PROJECTID is the id of the target project that request for the recommendation. This step will generate a binary-classification model as well as the predicted catch probability on each method for each its related exception.
  * Across-project recommendation: Run the `across.py` script in `train` directory as `python across.py --train_id=PROJECTID`.
  * Intra-project recommendation: Run the `intra.py` script in `train` directory as `python intra.py --train_id=PROJECTID`.
  * Intra-project+ recommendation: Run the `intraplus.py` script in `train` directory as `python intraplus.py --train_id=PROJECTID`.
* Run the `top.py` script in `train` directory to get the recommendation result and the recommendation performance in terms of SuccRate@1,2,3.
* Other:
  * You can run the `calcImportance.py` script in `FeatImportanceAnalysis` directory to get the importance score of each type of featres with AutoGluon.
  * You can specify the training time limit and the output directory for AutoGluon through changing the value of `time_limites` and `oupput_directory` in `across.py`, `intra.py`, `intraplus.py`, and `calcImportance.py`.

-------

### Appendix: List of the 29 Open-Source Java Projects in the Experiment

| Project Abbr. | Project Full Name            | Repository Link                                    | Version         |
| ------------- | ---------------------------- | -------------------------------------------------- | --------------- |
| C3P0          | Swaldman C3P0                | https://github.com/swaldman/c3p0                   | 0.9.5.5         |
| Collections   | Apache Commons Collections | https://github.com/apache/commons-collections      | 4.5             |
| DBCP          | Apache Commons DBCP         | https://github.com/apache/commons-dbcp             | 2.7.1           |
| FastJson      | alibaba fastjson             | https://github.com/alibaba/fastjson                | 1.2.63          |
| Gson          | Google Gson                  | https://github.com/google/gson                     | 2.8.7           |
| Guava         | Google Guava                 | https://github.com/google/guava                    | HEAD-jer        |
| HttpClient    | Apache HttpComponents Client | https://github.com/apache/httpcomponents-client    | 5.0.1           |
| Jackson       | FasterXML jackson            | https://github.com/FasterXML/jackson               | 3.0.0           |
| Logging       | Apache Commons Logging     | https://github.com/apache/commons-logging          | 1.2.1           |
| Dubbo         | Apache Dubbo                 | https://github.com/apache/dubbo                    | 2.7.5           |
| Flink         | Apache Flink                | https://github.com/apache/flink                    | 1.11            |
| Grizzly       | JaveEE Grizzly               | https://github.com/javaee/grizzly                  | 2.4.5           |
| HikariCP      | brettwooldridge HikariCP     | https://github.com/brettwooldridge/HikariCP        | 3.4.2           |
| Jersey        | Javaee Jersey                | https://github.com/javaee/jersey                   | 2.31            |
| JPA           | Spring Data Jpa              | https://github.com/spring-projects/spring-data-jpa | 2.3.0           |
| Log4J         | Apache Log4J                | https://github.com/apache/log4j                    | 2.13.1          |
| MyBatis       | MyBatis                      | https://github.com/mybatis/mybatis-3               | 3.5.5           |
| Shiro         | Apache Shiro                | https://github.com/apache/shiro                    | 1.5.2           |
| SLF4J         | Qos-ch Slf4j                 | https://github.com/qos-ch/slf4j                    | 2.0.0           |
| Storm         | Apache Storm                | https://github.com/apache/storm                    | 2.2.0           |
| Struts        | Apache Struts               | https://github.com/apache/struts                   | 2.6             |
| XNIO          | XNIO XNIO                    | https://github.com/xnio/xnio                       | 3.8.0           |
| ActiveMQ      | Apache ActiveMQ              | https://github.com/apache/activemq                 | 5.16.0 |
| Druid         | Alibaba Druid                | https://github.com/alibaba/druid                   | 1.1.20          |
| HBase         | Apache HBase                | https://github.com/apache/hbase                    | 3.0.0           |
| Netty         | Netty Netty                  | https://github.com/netty/netty                     | 4.1.44          |
| RocketMQ      | Apache RocketMQ             | https://github.com/apache/rocketmq                 | 4.6.1           |
| Tomcat        | Apache Tomcat               | https://github.com/apache/tomcat                   | 9               |
| ZooKeeper     | Apache ZooKeeper            | https://github.com/apache/zookeeper                | 3.6.0           |