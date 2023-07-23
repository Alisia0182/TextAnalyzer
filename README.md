# TextAnalyzer_Spark
WordGraph

In this project, Apache Spark is used to analyze a text dataset. Specifically, we will build
a directed graph where the vertices are words in the dataset, and edges are directed from a predecessor
to a successor in a pair of co-occuring words (i.e. words which are adjacent in the text). Furthermore,
each edge will be weighted by the frequency of that co-occurence. On large datasets this can be a
costly task, however we will map-reduce based distributed computations to accelerate this.
