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
operations against the data therein.  The shape of the mechanics is
coded here.  The basic share is... group data by some criteria, and
then within that group apply a statistical function to get aggregated
information.

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

## Points of note:

There is a conspicuous lack of testing code here, mea culpa.  I just
didn't have time to make meaningful tests.  It is not clear to me what
the testing suite would look like for this simple program.

I chose to represent each file in a map datastructure that is not
limited to the scope of a function but is present throughout the
namespace.  Global scope references held in `atoms`.

Also the separation
between the mechanics / manipulations of data is something that made
this code more portable and modular.  The statistic functions have no
coupling with how they are used.  Also the mechanics being performed
is captured, such that each group, and operations on that group, are
separated.

One interesting bit was how to represent (and label) the output data.
There is a small amount of <i>presentation</i> code in the processing
function.  It needs to be there to create the right keys to label the
output (the headers).  There are optional <i>keyword arguments</i>
that are provided to allow the caller to specify what the headings
will be.

Both the data manipulation and the writing out to file are done via
<i>lazy sequences</i> both fuctions are <i>threaded</i> into each other
to compose the final shape of how these parts of the program are
constructed.

``` clojure
(-> (median-age-by-dept) (capture-to-file "employee-age-by-department.csv") table))
```

## Efficiency

The code takes care to make use of vectors and maps for fast O(1) lookups.  Lazy sequenes are used to limit memory consumption.  Only what <u>needs</u> to be in the iteration loop is done.

There is a fair amount of exception handling as there is a malformed number present in the data and a null field (referencing a non-exisent department #8 (?).

## Makefile!? What? ;-)

I like to use `make` as the lowest common denominator build tool.  I use the Makefile to generate the clojure `project.clj` file from a template (nothing fancy, juuuust enough - using `sed`.  This means that there is only one place for specifying information about the project. The make targets essentially delegate to [leiningen](https://leiningen.org/).  Also the latest `lein` script is in the bin directory of this project and referenced in the Makefile.  With make as the base, we can support other languages build artifacts using it in the situation of a polyglot environment.  The Makefile also runs the code. <i>(there is an interesting issue where the JVM does not immediately exit but hangs there for 30 seconds, not sure why just yet, but smells like a thread got launched off that has to time out Hmmmm....)</i>

## Conclusion

The code is clean, efficient and thread safe.

### Oh Clojure...

I decided to write this in Clojure because the language lends itself to easy data manipulation.  My Java 8 coding is less practiced than my Clojure coding (time was a factor)
