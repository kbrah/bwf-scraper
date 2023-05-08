(ns bwf.scraper
  (:gen-class)
  (:require
   [clojure.string :as str]
   [clj-http.client :as client]
   [net.cgrand.enlive-html :as html]))

(def rankings-url "https://bwf.tournamentsoftware.com/ranking/category.aspx?id=34067&category=472")
(def bwf-base-url "https://bwf.tournamentsoftware.com")

(def website-content-rankings
  (html/html-resource (java.net.URL. rankings-url)))

; (pprint/pprint website-content-rankings)

(defn match-times [profile-url year]
  (as-> (str bwf-base-url profile-url "/tournaments/" year) v
    (java.net.URL. v)
    (html/html-resource v)
    (html/select v [:.tag :time :> html/text-node])
    (map (fn [time] (as-> time t
                      (str/split t  #" ")
                      (map (fn [time-part]
                             (if (str/includes? time-part "h")
                               (* (Integer/parseInt (str/replace time-part #"h" "")) 60)
                               (Integer/parseInt (str/replace time-part #"m" "")))) t)
                      (reduce + t))) v)
    (float (/ (reduce + v) (count v)))))

(def players
  (map (fn [row]
         (def profile-url (get-in (first (html/select row [[(html/nth-child 6)] :> :a])) [:attrs :href]))
         {:rank (first (:content (first ((nth (row :content) 1) :content))))
          :points (->> (html/select row [[(html/nth-child 8)]])
                       (first)
                       (:content)
                       (first))
          :profile-url  profile-url
          :name (first (:content (first (html/select row [[(html/nth-child 5)] :> :a]))))
          :avg-match-len (match-times profile-url "2022")})
       (->>
        (html/select website-content-rankings [:tr])
        (drop-last 1)
        (drop 2))))
; ((client/get (str bwf-base-url ((first players) :profile-url))) :body)

(println players)
