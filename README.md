# TextAnalyzer_Spark

In this project, Apache Spark is used to analyze a text dataset. Specifically, we will build
a directed graph where the vertices are words in the dataset, and edges are directed from a predecessor
to a successor in a pair of co-occuring words (i.e. words which are adjacent in the text). Furthermore,
each edge will be weighted by the frequency of that co-occurence. On large datasets this can be a
costly task, however we will map-reduce based distributed computations to accelerate this.

## 1 Implementing the Word Graph
Build a table that represents a graph giving the frequncies of words occurring adjacently
in a text. The data set for this problem is the novel Pride and Prejudice. A collection of files (one
per chapter) is provided in the course Github. The occurrences are calculated in individual lines; your
program does not need to intelligently separate sentences because the Mapper function is invoked for
each newline by default.

When two words are adjacent to each other in a sentence we will refer to first as the predecessor
and the second as the successor. 

## 2 Creating a Spark Environment
To help with your implementation, a Docker image has been created with Spark and other relevant
dependencies. Set up the Docker image and run the code template
in a Docker container.
