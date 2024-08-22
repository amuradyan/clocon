(ns pleajure.core
  (:gen-class))

(defn ???
  []
  (throw (ex-info "Not implemented" {})))

(declare interpret-list)

(defn atom? [subject]
  (or
   (instance? clojure.lang.Symbol subject)
   (instance? clojure.lang.Keyword subject)
   (instance? clojure.lang.Atom subject)))

(defn consider-entry
  [entry]
  (cond
    (not (list? entry)) [:error :entry-is-not-a-pair]
    (not (atom? (first entry))) [:error :entry-name-is-not-atom]
    (not (= (count entry) 2)) [:error :entry-is-not-a-pair]
    :else [:valid-entry entry]))

(defn interpret
  [form]
  (cond
    (symbol? form) [:keyword (keyword form)]
    (string? form) [:string form]
    (number? form) [:number form]
    (list? form) (interpret-list form)
    :else [:error :unknown-form form]))

(defn interpret-list
  ([form]
   (if (empty? form)
     [:list []]
     (interpret-list form [] {} true)))

  ([form list-instance map-instance probable-map?]
   (cond
     (empty? form) (if probable-map?
                     [:map map-instance]
                     [:list list-instance])
     :else (let
            [[current & rest] form
             [errors? _] (consider-entry current)
             still-probable-map? (and probable-map? (not (= errors? :error)))]
             (if still-probable-map?
               (let [[_ interpreted-name] (interpret (first current))
                     [_ interreted-value] (interpret (second current))
                     updated-list-instance (conj list-instance [interpreted-name interreted-value])
                     updated-map-instance (assoc map-instance interpreted-name interreted-value)]
                 (interpret-list rest updated-list-instance updated-map-instance still-probable-map?))
               (let [updated-list-instance (conj list-instance ((comp second interpret) current))]
                 (interpret-list rest updated-list-instance map-instance still-probable-map?)))))))

(defn parse-config
  [config]
  (let [[status value] (interpret config)]
    (case status
      :entry [:config value]
      [:error :invalid-config])))

(defn -main
  [& _]
  (println (load-string (str "'" (slurp "resources/test.plj")))))
