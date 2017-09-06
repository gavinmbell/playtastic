# playtastic

A [Clojure](https://clojure.org/) library designed to meet the requirements of the task prescribed.  Please see the [discussion](doc/discussion.md) write-up to get an understanding of the design and rationale behind the code.

## Prerequisites

In order to build and run the artifact you must have the following in the environment
 * Java JDK1.8
 * gnu `make`
 * `unzip`
 * `sed`
 * `git`

## Usage

Create the artifact (the jar file)...

``` shell
%> make
```
The output should look something like this...<br>
(caveat: if you don't already have Clojure on your box it will download the internet first)

``` shell
Generating project.clj from template...
./bin/lein clean
./bin/lein compile
Compiling playtastic.core
LEIN_SNAPSHOTS_IN_RELEASE=1 ./bin/lein with-profile user uberjar
Compiling playtastic.core
Compiling playtastic.core
Created <where you checked it out>/playtastic/target/playtastic-0.1.0-SNAPSHOT.jar
Created <where you checked it out>/playtastic/target/playtastic-0.1.0-SNAPSHOT-standalone.jar
```

To run the jar using the data files packed within (see resources dir)...

``` shell
%> DATA_DIR=<directory of data files> make run
```

The output...

``` shell
java -jar target/playtastic-0.1.0-SNAPSHOT-standalone.jar
Sep 07, 2017 9:26:17 PM clojure.tools.logging$eval1$fn__4 invoke
INFO: Loading data...
Sep 07, 2017 9:26:17 PM clojure.tools.logging$eval1$fn__4 invoke
INFO: Running reports - see output *.csv files.
Sep 07, 2017 9:26:17 PM clojure.tools.logging$eval1$fn__4 invoke
INFO: Median income by department
Sep 07, 2017 9:26:17 PM clojure.tools.logging$eval1$fn__4 invoke
WARNING: Bad input value  For input string: "4470.0O"
+------------------------+---------------+
| dept                   | median-salary |
+------------------------+---------------+
| Accounting             | 3920.0        |
| Business Development   | 3070.0        |
| Human Resources        | 3100.0        |
| Information Technology | 2940.0        |
| Marketing              | 3470.0        |
| Public Relations       | 2670.0        |
| Sales                  | 2265.0        |
+------------------------+---------------+
Sep 07, 2017 9:26:17 PM clojure.tools.logging$eval1$fn__4 invoke
INFO: 95th percentile income by department
Sep 07, 2017 9:26:17 PM clojure.tools.logging$eval1$fn__4 invoke
WARNING: Bad input value  For input string: "4470.0O"
+------------------------+----------------------+
| dept                   | 95-percentile-salary |
+------------------------+----------------------+
| Accounting             | 4440.0               |
| Business Development   | 4060.0               |
| Human Resources        | 3730.0               |
| Information Technology | 3820.0               |
| Marketing              | 4010.0               |
| Public Relations       | 2670.0               |
| Sales                  | 2940.0               |
+------------------------+----------------------+
Sep 07, 2017 9:26:17 PM clojure.tools.logging$eval1$fn__4 invoke
INFO: Average income by age ranges with factor of ten
Sep 07, 2017 9:26:17 PM clojure.tools.logging$eval1$fn__4 invoke
WARNING: Bad input value  For input string: "4470.0O"
+-----------+--------------------+
| age-range | average-salary     |
+-----------+--------------------+
| 10        | 2190.0             |
| 20        | 3856.470588235294  |
| 30        | 2895.525           |
| 40        | 3043.5714285714284 |
| 50        | 2269.285714285714  |
| 60        | 3237.5             |
| 70        | 2986.6666666666665 |
| 80        | 2220.0             |
+-----------+--------------------+
Sep 07, 2017 9:26:17 PM clojure.tools.logging$eval1$fn__4 invoke
INFO: Median employee age by department
+------------------------+------------+
| dept                   | median-age |
+------------------------+------------+
| Accounting             | 38.0       |
| Business Development   | 49.0       |
| Human Resources        | 37.5       |
| Information Technology | 50.0       |
| Marketing              | 42.0       |
| Public Relations       | 52.0       |
| Sales                  | 56.0       |
+------------------------+------------+
```

To clean everything out


```shell
%> make clean-all
```

To generate the documentation for this project

``` shell
%> make doc
```
(look at the generated target/doc/index.html)

## Files

There are three commma separated files in `resources` directory:

 * departments.csv  - list of departments
 * employees.csv    - first column contains position of department in alphabetically sorted department list, followed by employee name and salary
 * ages.csv         - first column contains employee name, followed by age

## Challenge

Write a program that will generate the following reports in corresponding files.

 * income-by-department.csv - median income by department
 * income-95-by-department.csv - 95-percentile income by department
 * income-average-by-age-range.csv - average income by age ranges with factor of ten
 * employee-age-by-department.csv - median employee age by department

Reports must be generated in a comma separated format with header columns.

## Conditions

 * Code should be compilable with Oracle JDK 1.8 and run with path to directory containing data files as first parameter.
 * <strike>Only libraries that are part of Oracle Java Runtime are allowed in production code.</strike>

 This code was written in [Clojure](https://clojure.org/).  The
 corresponding Java *8* code would be similar, using the <i>stream</i>
 facility.


## License

Copyright © 2017 Gavin M. Bell / MIT License

[![Clojure](https://clojure.org/images/clojure-logo-120b.png)](http://clojure.org)
