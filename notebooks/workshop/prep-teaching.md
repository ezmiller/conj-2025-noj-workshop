
### Point 1 - Loading with :key-fn (when showing the def raw-data):

"We use :key-fn keyword to convert column names to keywords. So 'FISCAL_YEAR' becomes :FISCAL_YEAR. This makes them easier to work with - you can use them as functions, like (:FISCAL_YEAR row) instead of (get row "FISCAL_YEAR")."

### Point 2 - tc/select-rows vs Pandas (when showing the filter):

"In Pandas you'd write df[df['FISCAL_YEAR'] >= 2020] with boolean indexing. Here we're more explicit: we pass a predicate function that returns true/false for each row. It's a bit more verbose but clearer about what's happening."

### Point 3 - The threading macro (when showing the group-by check):

"The -> threading macro makes this read top-to-bottom: take workshop-data, group by fiscal year, aggregate to count rows, order by year. Each step takes the result of the previous step as its first argument."

### Point 4 - Notice what we're NOT doing (at the end):

"Notice we only created two defs: raw-data and workshop-data. In a typical Python notebook you might see df, df_filtered, df_recent, df_clean, etc. We're keeping our namespace clean by only defining what we'll reuse."