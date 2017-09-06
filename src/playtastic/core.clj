(ns playtastic.core
  (:require [clojure.java.io :refer :all]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [table.core :refer [table]])
  (:gen-class))

;; TODO: Make a better...

;; (if you are reading any of this I am sorry... you see I got a bit
;; carried away with 'ALL OF THE THINGS'.  There is just a lot of
;; stuff I want to do and touch and play with... I'll prolly jot down
;; some notions on the way - just for myself.  Pardon the ramblings of
;; a MAD MAN :-P... that might just take over the world <evil laugh>)

;; -----------
;; Misc notes:
;; -----------
;; TODO: As we are dealing with computation over currency we should probably
;; treat them specially and use a currency handling numeric library
;; such as:
;; https://github.com/clojurewerkz/money

;;-------
;; Data Description
;;-------
;; ages.csv -------- first column contains employee name, followed by age
;; departments.csv - list of departments
;; employees.csv --- first column contains position of department in
;;                   alphabetically sorted department list, followed by employee name and salary
;;-------

;;-------
;; Challenge Description
;;-------
;; Write a Java program that will generate the following reports in corresponding files.
;;
;; income-by-department.csv - median income by department
;; income-95-by-department.csv - 95-percentile income by department
;; income-average-by-age-range.csv - average income by age ranges with factor of ten
;; employee-age-by-department.csv - median employee age by department
;;
;; Reports must be generated in a comma separated format with header columns.
;;-------

;;--------------------
;; Data Loading Datastructure Building Functions
;;--------------------
(def ages (atom {}))

(defn load-ages [filename] ;; "/Users/cue/projects/playtastic/resources/ages.csv"
  (with-open [rdr (reader filename)]
    (reduce (fn [acc [k v]]
              (assoc acc (str/lower-case k) v)) {} (map #(str/split  %1 #",") (line-seq rdr)))))

(defn reload-ages! [dirname]
  (let [filename (str (file dirname "ages.csv"))]
    (reset! ages (load-ages filename))))

;;---

(def departments (atom {}))

(defn load-departments [filename] ;; "/Users/cue/projects/playtastic/resources/departments.csv"
  (with-open [rdr (reader filename)]
    (reduce (fn [acc v]
              (assoc acc (inc (count acc)) v)) {} (line-seq rdr))))

(defn reload-departments! [dirname]
  (let [filename (file dirname "departments.csv")]
    (reset! departments (load-departments filename))))

;;---

(def employees-norm (atom {}))

(defn load-employees [filename] ;; "/Users/cue/projects/playtastic/resources/employees.csv"
  (with-open [rdr (reader filename)]
    (reduce (fn [acc [_  dept-num employee-name, gender, salary]]
              (let [employee-key (str/lower-case employee-name)]
                (conj acc {:dept (get @departments (Integer. dept-num))
                           :name employee-name
                           :age (get @ages employee-key)
                           :gender gender
                           :salary salary}))) [] (filter identity (map #(re-matches #"([^,]*),([^,]*),([^,]*),([^,]*)" %1) (line-seq rdr))))))

(defn reload-employees! [dirname]
  (let [filename (str (file dirname "employees.csv"))]
    (reset! employees-norm (load-employees filename))))


;;--------------------
;; Statistics Utility Functions
;;--------------------

(defn average
  "Computes the average of a sequence of numbers or numeric strings"
  [items]
  (let [items-count (atom (count items))]
    (/ (reduce + (filter identity (map (fn [item-value]
                                         (try
                                           (Float. item-value)
                                           (catch Exception e
                                             (log/warn "Bad input value "(.getMessage e))
                                             (if (> @items-count 1) (swap! items-count dec)) ;;avoid divide by zero in particular circumstances
                                             nil))) items))) @items-count)))

(defn median
  "Computes the median of a sequence of numbers or numeric strings"
  [items]
  (let [items-count (atom (count items))
        items-as-numbers (filterv identity (map (fn [item-value]
                                                  (try
                                                    (Float. item-value)
                                                    (catch Exception e
                                                      (log/warn "Bad input value "(.getMessage e))
                                                      (swap! items-count dec)
                                                      nil))) items))]
    (if (odd? @items-count)
      (nth items-as-numbers (/ @items-count 2))
      (/ (+ (nth items-as-numbers (/ @items-count 2)) (nth items-as-numbers (dec (/ @items-count 2)))) 2))))

(defn percentile
  "Computes the value of the nth percentile (uses the Nearest-Rank method)
  see: https://en.wikipedia.org/wiki/Percentile#Nearest_rank
  Takes the percentile value to compute and the input sequence of numbers."
  [percentile-value items]
  (let [items-count (atom (count items))
        items-as-numbers (filterv identity (map (fn [item-value]
                                                  (try
                                                    (Float. item-value)
                                                    (catch Exception e
                                                      (log/warn "Bad input value "(.getMessage e))
                                                      (swap! items-count dec)
                                                      nil))) items))]
    (cond
      (<= percentile-value 0) (first items-as-numbers)
      (>= percentile-value 100) (last items-as-numbers)
      :else (let [values (sort items-as-numbers)
                  N (count values)
                  percentile-index (dec (Math/ceil (* (/ percentile-value 100) 10)))]
              (if (= 1 N) (first values) (nth values percentile-index))))))

;;--------------------
;; Query Functions
;;--------------------

(defn- op-onto-field-by-group
  "Primary higer order function that performs the basic task of
  grouping and then mapping over selected values within that group.
  The initial grouping is done with a simple group-by operation.  For
  each of the maps in the collection (representing each record
  associated with the group-by value) a map operation is done applying
  the operation-fn and then run through an identity filter to
  drop nil groups There are optional keyword arguments that allow for
  some control over the labeling of the output datastructure.

  It is a question of interpretation if the identity filter step is desired.
  (There is a small hack - when you pass in custom group-by functions
  you need to set the group-key-label keyword argument or there will
  be nil as the key)"

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

;;-------
;; End user data processing User Functions that delegate to higher order function to orchestrate the mechanics.
;;-------
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

;;--------
;; Writes Maps to CSV file
;;--------
(defn capture-to-file [data filename]
  (if filename
    (with-open [wtr (writer filename :append false)]
      (.write wtr (str (clojure.string/join "," (map name (keys (first data)))) "\n"))
      (doseq [entry data]
        (.write wtr (str (clojure.string/join ","(vals entry)) "\n")))))
  data)

;;--------
;; Loading Data...
;;--------
(defn reload-all!
  "Loads (initially) or reloads the data from the data files into the local datastructures"
  [dirname]
  (log/info "Loading data from "dirname)
  (reload-ages! dirname)
  (reload-departments! dirname)
  (reload-employees! dirname))

;;--------
;; Removing / Clearing Data...
;;--------
(defn clear-all
  "Clears all the data structures (note in reverse loading order to obviate that fault mode)"
  []
  (log/info "Clearning data...")
  (reset! employees-norm {})
  (reset! departments {})
  (reset! ages {}))

;;--------
;;Answering the challenge: https://github.com/hubrick/hubrick-backend-challenge
;;--------
(defn run-reports
  "Runs the reports and generates the output files as specified in the challenge"
  []
  (log/info "Normalized Employee Table")
  (table @employees-norm)
  (log/info "Running reports - see output *.csv files.")
  (log/info "Median income by department")
  (-> (median-income-by-dept) (capture-to-file "income-by-department.csv") (table))
  (log/info "95th percentile income by department")
  (-> 95 (percentile-income-by-dept) (capture-to-file "income-95-by-department.csv") table)
  (log/info "Average income by age ranges with factor of ten")
  (-> (average-income-by-age-range) (capture-to-file "income-average-by-age-range.csv") table)
  (log/info "Median employee age by department")
  (-> (median-age-by-dept) (capture-to-file "employee-age-by-department.csv") table))

(defn -main [& args]
  (reload-all! (first args))
  (run-reports))


;; TODO: nuke this stuff
;;-----
;; misc scratch work as I was poking around
;;-----
#_(table (sort-by (juxt :dept :age) @employees-norm))
#_(map (fn [[dept-name entries]]
         (println "department name is: "dept-name "count " (count entries)
                  (sort (map :salary  (seq entries))))) (sort-by first (group-by (fn [entry] (* (quot (Integer. (:age entry)) 10)10)) @employees-norm)))
