# Daily Log App
### Database schema (PostgreSQL)
`logs` table:
`activity_id | date | value`
primary key is `activity_id, date`.
`activity_id` is foreign key.

`activities` table:
`activity_id | description | type`
`activity_id` is primary key.
`description` should be unique.
`type` will be an enum e.g. `int`, `bool`, `float`, `percentage`

We don’t want to use `description` as the unique identifier in`activities` and foreign key in `counts` because we might want to allow the user to change the description.

### Clojure server
Two endpoints: GET and PUT.

GET will fetch all `logs` for a date range, joined on `activities` to get `description` and `type`.

Straight from DB this will look like:
```
[{:activity-id :a
  :desc “Took vitamins?”
  :date :2020-03-31
  :value 1
  :type :bool}
 {:activity-id :b
  :desc “KM run”
  :date :2020-03-30
  :value 123
  :type :float}
 {:activity-id :c
  :desc “Coffees drunk”
  :date :2020-03-31
  :value 3
  :type int}]
```

/?Next we apply grouping to suit the front end?/

### Re-frame client
db schema:
```
{:date-range-start  :2020-03-28 ;; inclusive
 :date-range-end    :2020-04-03 ;; inclusive
 :date-being-edited :2020-03-31 ;; defaults to today
 :logs {:2020-03-31 {:a {:type :bool
                         :value 1
                     :c {:type :int
                         :value 3}}
        :2020-03-30 {:b {:type :float
                         :value 123}}}
```

Components will subscribe to DB to get `date-being-edited`. 

Will use this to construct a `visible-date-range` being 2 days either side (6 days total, we will only display 3 days total on smaller displays).

We will subscribe to the DB to get `visible-activity-ids` based on `visible-date-range`.

We will subscribe to the DB to get `visible-logs` base on `visible-date-range` and `logs`.

We will subscribe to the DB to get `activity-names`, being a lookup of `activity-id` -> `name`

We will display a header, being the dates in `visible-date-range`.

We will display a row for each `visible-activity-ids`, with 6 (3 on small displays) columns. First column will be the `activity-name` taken directly from `visible-activity`. Remaining columns will be the values taken from `visible-logs` using a lookup on `activity-name` and the `date`.

##### Editing
Could store edits in an “overlay” layer under a separate `:edits` key in the DB. The first time the user changes a value, this new value is translated into the integer value (e.g. true -> 1) and stored under the same path as in `:logs`.

The subscription would need to overlay this data on top of `:logs` to get the correct display values.

After allowing a short amount of time to elapse after an edit, we would take all the edits in the DB and send them via PUT request to the back end. We would receive a status code back for each `date`, `activity-id` and if a 200 is received we remove that entry from `:edits` and move it to `:logs`, overwriting the old value.

In the meantime we can provide feedback on the front end via highlighting or similar to show that the value hasn't been persisted yet.

If we receive a non-200 back, we flash a warning and reset the value (i.e. clear it from the `:edits`.
