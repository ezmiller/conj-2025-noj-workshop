# Charlotte 311 Workshop - Live Coding Guide (Minimal v2)

**Total Guided Duration:** ~55-65 minutes
**Structure:** Theory (8-10 min) → Prep (10-12 min) → Exploration (35-40 min)

---

## SECTION 0: Theory - Why Datasets? (8-10 min)

### Transitions
- "Before we dive into real data, let's understand why we're using datasets"
- "Watch this - same operation, different data structures"
- "Now let's look under the hood at what a dataset actually is"

### 🎯 TEACHING MOMENT #1 - Performance Matters
**Why datasets are fast:**
- No redundant storage of column names in each row
- Avoids explosion of intermediate copies
- Strongly typed, packed in continuous memory
- Further optimizations at the column level

**What to Say:**
- "For small data, regular Clojure structures are fine"
- "But at scale, these optimizations really matter"
- "We're not saying Clojure is slow. Sometimes you really want a vector that can take just anything. We're saying datasets are optimized for this use case"

### 🎯 TEACHING MOMENT #2 - Datasets ARE Maps
**Tour the type system:**

1. Dataset type → `tech.v3.dataset.impl.dataset.Dataset`
2. But: `(map? ds-data)` → `true`
3. Keys are column names: `(keys ds-data)`
4. Values are columns: `(vals ds-data)`
5. Rows are map-like: `(-> ds-data (tc/rows :as-maps) first)`
6. But actually FastStruct views: `(-> ds-data tc/rows first type)`
7. Columns have types: `(tcc/typeof column)`

**What to Say:**
- "It ACTS like a map - column-oriented but map-like ergonomics"
- "Under the hood: packed, typed arrays for efficiency"
- "You get the best of both worlds"

### Properties to Demonstrate
- Columns are countable: `(count column)`
- Random access: `(nth column 5)`
- But optimized storage underneath

---

## SECTION 1: Prep - Loading & Basic Operations (10-12 min)

### What to Do
1. Create simple dataset from map: `{:A [1 2 3] :B [4 5 6]}`
2. Load raw Charlotte 311 data
3. Use `tc/info` to see what we have (2017-2028 data)
4. Filter to reasonable years with `tc/select-rows`
5. Group and count by FISCAL_YEAR
6. Sample 30k per year and write workshop dataset

### Transitions
- "Now let's work with real data - Charlotte's 311 service requests"
- "First, a quick look at what we have"
- "Let's create a manageable workshop dataset"

### 🎯 TEACHING MOMENT #3 - Row Functions Take Maps
**When using select-rows, point out:**
- "The selector function takes each row"
- "The row IS a map - we access it with keyword `:FISCAL_YEAR`"
- "This is that map-like interface we just talked about"

### 🎯 TEACHING MOMENT #4 - Threading Macros Introduction

**First pipeline:**
**THIS IS CRITICAL - Emphasize:**
- "Notice the `->` arrow - this is threading"
- "Data flows top to bottom through functions"
- "At each step: returns a NEW dataset"
- "Each function takes dataset as FIRST argument"
- "By design! This is functional data science"

**Contrast with Pandas:**
```python
# Python/Pandas
df = pd.read_csv('charlotte_311.csv')
df = df.groupby('FISCAL_YEAR').size().reset_index(name='COUNT')
df = df.sort_values('FISCAL_YEAR')
# You've lost the original! Need .copy() to preserve it
```

**What to Say:**
- "In Pandas, you're mutating `df` at each step"
- "To preserve it, you need explicit `.copy()`"
- "Here: nothing mutates. Each step returns NEW dataset"
- "We avoid variable soup without sacrificing the original"

### Observation to Share
- "Our data is balanced: 295k-355k requests per year"
- "Still 2M rows total - let's sample it down"

---

## SECTION 2: Exploration - Finding Patterns (35-40 min)

### Step 2.1: Basic Exploration (5-7 min)

**What to Do:**
1. Load the workshop dataset (already sampled)
2. Group by REQUEST_TYPE and count
3. Order by count descending, show top 10
4. Create bar chart visualization

**Transitions:**
- "Now we have a manageable dataset - let's explore it"
- "What types of requests are most common?"

**🎯 TEACHING MOMENT #5 - No Variable Soup (REINFORCE)**
- "Notice: we didn't save intermediate results"
- "Just one clear pipeline from data to visualization"
- "No `df2`, `df3`, `df4` cluttering our namespace"

---

### Step 2.2: Time Series Setup (8-10 min)

**What to Do:**
1. Show available columns, point out RECEIVED_DATE
2. Create java-time formatter
3. Test parser on first date
4. Create top-five-requests set for filtering

**Transitions:**
- "Now let's look at these requests over time"
- "First we need to parse dates"

**Decision Point: When to `def`:**
- "We `def` the top-five set because we'll reuse it"
- "Most things stay in pipelines"

---

### Step 2.3: Monthly Time Series (10-12 min)

**What to Do:**
1. Create date rollup function (first day of month)
2. Test the function
3. Build complete pipeline: filter → add column → group → aggregate
4. Show inline FIRST, then `def` it as `year-month-data`
5. Create basic line chart
6. Improve formatting (legend position)

**Transitions:**
- "To see patterns, we need monthly buckets"
- "Let's build the full pipeline"
- "Since we'll reuse this, let's name it"

**🎯 TEACHING MOMENT #6 - Immutability (THE BIG REVEAL)**

After creating `year-month-data`:
1. Show `workshop-data` is unchanged
2. Run a different query on `workshop-data`
3. Show it AGAIN - still unchanged!

**What to Say:**
- "In Pandas I'd be terrified - did I mutate something?"
- "Here: `workshop-data` never changes"
- "Every operation returns NEW dataset"
- "Structural sharing makes this efficient - no copying"

---

### Step 2.4: Seasonal Patterns (8-10 min)

**What to Do:**
1. Create month-only aggregation (collapse years)
2. Use `map-columns` (show alternative to `add-column`)
3. Group by REQUEST_TYPE and MONTH
4. Visualize seasonal pattern

**Transitions:**
- "What about seasonal patterns across all years?"
- "Let's collapse into a single seasonal cycle"

**🎯 TEACHING MOMENT #7 - Data Normalization**
- "When we collapsed years, the pattern changed significantly"
- "COVID year (2020) was flattening the cycles"
- "Can't blindly aggregate - context matters"

---

## WRAP-UP & TRANSITION (2-3 min)

### Key Takeaways to Emphasize

**From Theory:**
- Datasets are optimized for large-scale operations
- Map-like interface with columnar efficiency underneath

**From All Sections:**
1. **Functional Pipelines** - `->` chains, not variable soup
2. **Immutability** - originals never change, no `.copy()` overhead
3. **Map-like Interface** - datasets/rows/columns all feel native
4. **Namespace Hygiene** - only `def` what you'll reuse
5. **Performance** - structural sharing, typed columns, optimized ops

---

## TRANSITION TO PARTICIPANT EXPLORATION

### What to Say
"You've seen why datasets are fast, how they work, and how to use them. Now it's your turn. Pick a question and explore."

### Remind Them
- "Pipeline first, `def` only if you'll reuse"
- "Can't break the data - it's immutable"
- "`workshop-data` is always there, unchanged"

---

## QUICK REFERENCE

### Key Concepts
- **Row functions**: Take row map `(fn [row] ...)`
- **Column functions**: Take whole dataset `(fn [ds] ...)`
- **Threading**: `->` (first position) vs `->>` (last position)

### When to `def`?
"Will you use this result more than once?"
- YES → `def` it
- NO → inline pipeline

### Column Access
- `(:col-name dataset)` - most common
- `(get dataset :col-name)` - also works
- `(keys dataset)` - all column names
