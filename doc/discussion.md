# Discussion

## Philosophy

The general approach is to break the task into <i>composable</i> functional units.  There is a bias toward putting specific end user information further out, closer to the user API functions.  This encourages functions to be more pure and general, thus capturing a singular purpose.  It is through composition where we create the right constructs for solving the user end problem.  This is demonstrated here and described below.  Clearly we should not let the <i>perfect</i> be the enemy of <i>progress</i> and thus we can/should refactor where can ;-).

## Approach

The code is broken into logical sections.

* [Loading the data from datafiles](https://github.com/gavinmbell/playtastic/blob/master/src/playtastic/core.clj#L46-L91)

The prescribed files are contained inside the jar artifact.  Files are
read and loaded into map data structures.  Each of the individual
files are used to create a normalized data structure representing the
resolving of the foreign references.  Operations for the rest of the
code is done against this normalized structure.

* [Statistic utility functions](https://github.com/gavinmbell/playtastic/blob/master/src/playtastic/core.clj#L92-L143)

Each of the statistical operations are implemented namely; `average`, `mean` and `percentile`.

* [Query functions](https://github.com/gavinmbell/playtastic/blob/master/src/playtastic/core.clj#L144-L177)

These functions directly manipulate the main data structure and apply
operations against the data therein.  The shape of the computation is
coded in `op-onto-field-by-group` function.  The basic shape is as
follows. First <b>group</b> data <b>by</b> some criteria, and then for
the data associated with each group apply a statistical function that
results in aggregated information. (i.e. `group-by` -> `map` -> `filter`)

``` clojure
[data operation-fn field-key group-key & {:keys [operation-fn-label field-key-label group-key-label] :or
                                            {field-key-label (keyword field-key)
                                             group-key-label (keyword group-key)}}]
  (let [operation-result-key-label (if (empty? operation-fn-label)
                                     (keyword field-key-label)
                                     (keyword (clojure.string/join "-" [(name operation-fn-label) (name field-key-label)])))]

    (filter group-key (map (fn [[group-name entries]]
                             (let [operation-result (operation-fn (sort (map field-key (vec entries))))]
                               (log/trace (name group-key) "name is:" group-name
                                          "has" (count entries) "entries,"
                                          (clojure.string/join " " (clojure.string/split (name operation-result-key-label) #"-")) "=" operation-result)
                               {group-key-label group-name
                                operation-result-key-label operation-result})) (sort-by first (group-by group-key data))))))
```

The interesting part here is that there is [one function](https://github.com/gavinmbell/playtastic/blob/master/src/playtastic/core.clj#L148-L177) that captures
the mechanics and then depending on the group field and the particular
field extracted and stats operation.  The particular combination of
these are [constructed with in-line functions calling the higher order
function](https://github.com/gavinmbell/playtastic/blob/master/src/playtastic/core.clj#L178-L191).

``` clojure
(def average-income-by-dept      #(op-onto-field-by-group @employees-norm (partial average)       :salary :dept :operation-fn-label "average"))
(def median-income-by-dept       #(op-onto-field-by-group @employees-norm (partial median)        :salary :dept :operation-fn-label "median")) ;; <--
(def average-age-by-dept         #(op-onto-field-by-group @employees-norm (partial average)       :age    :dept :operation-fn-label "average"))
(def median-age-by-dept          #(op-onto-field-by-group @employees-norm (partial median)        :age    :dept :operation-fn-label "median"))    ;; <--
(def percentile-income-by-dept   #(op-onto-field-by-group @employees-norm (partial percentile %1) :salary :dept :operation-fn-label (str %1"-percentile"))) ;; <--
(def average-income-by-age-range #(op-onto-field-by-group @employees-norm (partial average)       :salary (fn [entry] (try (* (quot (Integer. (:age entry)) 10)10)
                                                                                                                           (catch Exception e true))) ;; <-- small hack for filter phase
                                                          :operation-fn-label "average"
                                                          :group-key-label "age-range"))
(def median-income-by-age-range  #(op-onto-field-by-group @employees-norm (partial median)        :salary (fn [entry] (* (quot (Integer. (:age entry)) 10)10)) :operation-fn-label "median"))

```

Notice that the grouping is not only done by keyword, but had to be specified as functions.  For example age-range had to be computed.  Also the percentile function can take an argument for which percentile is desired.  We later call it with the value prescribed 95.

* [Output writing](https://github.com/gavinmbell/playtastic/blob/master/src/playtastic/core.clj#L192-L202)

Writing the output was prescribed by the requirements.  The files are written in the directory where the code is executed, and overwritten on each run.

* [Report running](https://github.com/gavinmbell/playtastic/blob/master/src/playtastic/core.clj#L225-L241)

Putting it all together, the operations, the data manipulation
mechanics and writing the data to file is captured by the [report
running function](https://github.com/gavinmbell/playtastic/blob/master/src/playtastic/core.clj#L225-L241).

``` clojure
(defn run-reports
  "Runs the reports and generates the output files as specified in the challenge"
  []
  (log/info "Running reports - see output *.csv files.")
  (log/info "Median income by department")
  (-> (median-income-by-dept) (capture-to-file "income-by-department.csv") (table))
  (log/info "95th percentile income by department")
  (-> 95 (percentile-income-by-dept) (capture-to-file "income-95-by-department.csv") table)
  (log/info "Average income by age ranges with factor of ten")
  (-> (average-income-by-age-range) (capture-to-file "income-average-by-age-range.csv") table)
  (log/info "Median employee age by department")
  (-> (median-age-by-dept) (capture-to-file "employee-age-by-department.csv") table))
```

* [helper user interaction functions](https://github.com/gavinmbell/playtastic/blob/master/src/playtastic/core.clj#L203-L224)

There are functions for overall realoading the data files and clearing the data out from the built state.

## Points of note: The Good :smile:, The Bad :unamused:, The Ugly ::japanese_ogre: ...

### The Bad :unamused:

There is a conspicuous lack of testing code here, mea culpa :-(.  I
just didn't have time to make meaningful tests out side of my REPL
testing (which admittedly is not <i>really</i> testing).  It is not clear to me what the
testing suite would look like for this simple program.

### The Ugly :japanese_ogre:

* There is [one place](https://github.com/gavinmbell/playtastic/blob/master/src/playtastic/core.clj#L186-L187) where the <i>filter</i> operation is being used that throws an exception when the group-by function is not a <i>keyword</i>

* We are doing mathematic operations on money so a money library like [Joda Money](http://www.joda.org/joda-money/) should probably be used.

* I could do a fancier job with handling of command line args (but that would be a bit overkill regarding the task at hand)

<i>I kept all the code in one source file for readability. Ideally I would have broken it out.</i>

### The Good :smile:

The layout of the code is cogent.  When structuring the constructs for
this task I chose to represent each bit of information as described in
the data files as a separate persistent data structuresthat are
wrapped in `atoms` in the namespace (pardon the Clojure speak). The
employee information is denormalized by resolving the join keys. The
resulting table is the basis for all the manipulations that take place
in the project.

There is a separation between the mechanics of data manipulation (processing) and the
data representation, this is something that makes this code more portable and
modular and flexible.  The statistic functions have no coupling with how they are
used.  Also the mechanics being performed is captured, such that each
group, and operations on that group, are separated.

One interesting bit was how to represent (and label) the output data.
There is a small amount of <i>presentation</i> code in the processing
function.  It needs to be there to create good keys to label the
output (the headers).  There are optional <i>keyword arguments</i>
that are provided to allow the caller to specify what the headings
will be.

Both the data manipulation and the writing out to file are done via
<i>lazy sequences</i>. Functions are
[<i>threaded</i>](https://clojuredocs.org/clojure.core/-%3E) into each
other to <i>compose</i> the final shape of how these parts of the
program are constructed.

``` clojure
(-> (median-age-by-dept) (capture-to-file "employee-age-by-department.csv") table))
```

## Efficiency

:smile: The code takes care to make use of vectors and maps for fast O(1)
lookups.  Lazy sequences are used to limit memory consumption.  And,
iterative code has only what <u>needs</u> to be in the iteration
present.

There is a fair amount of defensive coding as there is a malformed
number present in the data and a null field referencing a non-exisent
department (#8?).  :japanese_ogre: I could look at addressing some of
these issues without using exception handling, as exceptions are
expensive.

## Makefile!? What? :wink:

I know... I know... but... it is a
[good](https://www.merriam-webster.com/dictionary/good) thing to use
`make` as the lowest common denominator build tool.  I use the
Makefile to generate the clojure `project.clj` file from a simple
[template](https://raw.githubusercontent.com/gavinmbell/playtastic/master/etc/project.clj.tmpl)
(nothing fancy, juuuust enough - using `sed`).  This means that there
is only one place for specifying information about the project - the Makefile. The
`make` targets essentially delegate to
[leiningen](https://leiningen.org/).  The latest `lein` script is
in the bin directory of this project and referenced in the Makefile.
With `make` as the base building tool, we can support other languages' build tools
similarly to the `make` <-> `lein` interaction here.  Using `make` is
worthwhile in the context of a polyglot environment.  It provides a
stable <i>iterface</i> that CI/CD tools can use uniformly.  In this project there is a run target so that `make` also runs the code. (:japanese_ogre: <i>There is an
interesting issue where the JVM does not immediately exit but hangs
there for 30 seconds, not sure why just yet, but smells like a thread
got launched off that has to time out Hmmmm....</i>)

The resulting jar's MANIFEST.MF information is built reflecting
specified fields in the Makefile that are written to the project.clj
thus providing a clear accounting of how that artifact was built.

``` shell
Manifest-Version: 1.0
Repo-Site: https://github.com/gavinmbell/playtastic
Build-Date: Fri Sep  8 15:20:16 CEST 2017
Last-Commit-Author: Gavin M. Bell <gavin@dontspamme.org>
Release-Name: flatbush
Author: Gavin M. Bell
Built-By: gavin
Profile: user
Commit: fa5b3f98e0ce3d04e6d4fc02d4e2d8a4733bbcf3
Project: playtastic
Branch: master
Version: 0.1.0-SNAPSHOT
Main-Class: playtastic.core
Last-Commit-Date: Wed Sep 6 02:11:56 2017 +0200
Organization: Hubrick
Created-By: Leiningen 2.7.1
Build-Jdk: 1.8.0_66
```

## Conclusion

:smile: The code is clean, efficient and thread safe.

### Oh Clojure...

I decided to write this in Clojure because the language is extremely
expressive and lends itself to easy data manipulation.  My Java 8
coding is less practiced than my Clojure coding (time was a factor).
The solution in Java would be similar using the Java examples in the
[online trail](https://docs.oracle.com/javase/tutorial/collections/interfaces/collection.html).

[![Clojure](https://clojure.org/images/clojure-logo-120b.png)](http://clojure.org)
