# Dalap (Decide As Late As Possible) [![Build Status](https://secure.travis-ci.org/BirdseyeSoftware/dalap.png)](http://travis-ci.org/BirdseyeSoftware/dalap)

Dalap is an extremely flexible tree-walking / tree-transformation
library for Clojure and Clojurescript. It is the foundation of
dalap.html, an html templating/serialization library (similar to
`hiccup` and `enlive`), and lein-dalap, a
Clojure->Clojurescript code transformation tool.

The name is the acronym for Decide As Late As Possible, from lean
programming.

## Components

The namespace `dalap.walk` provides `Walker`, which is a wrapper
around a visitor function (a multimethod or a single function from a
Protocol). It simplifies the recursive invocation of the visitor over
a seq of objects (possibly nested) and makes it possible to:

 a) decorate or replace the visitor for sub-regions of the tree and 

 b) maintain traversal state if needed.

The namespace `dalap.rules` implements a css-like selector+transformer
rules system, which allows you to selectively transform portions of a
tree.  See [lein-dalap][lein-dalap] for an example of this in action.

## Usage

See the test suite or the packages [dalap.html][dalap-html] and
[lein-dalap][lein-dalap] for usage examples.

## License

Copyright (C) 2012 Birdseye Software Inc.

Distributed under the MIT License.

[dalap-html]:https://github.com/BirdseyeSoftware/dalap-html
[lein-dalap]:https://github.com/BirdseyeSoftware/lein-dalap
