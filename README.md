# Dalap (Decide As Late As Possible)

Dalap is an extremely flexible serialization / template library for
Clojure. 

The name is the acronym for Decide As Late As Possible, from lean
programming.

On the surface, it is similar to the hiccup library and hiccup
templates are valid input for Dalap. However, there are several key
differences:

* Dalap html-escapes all strings, unlike hiccup.

* Dalap provides a selector/transformer system similar to the css
  selectors/transformers in Enlive.

* Dalap is designed on the philosophy that templates should be
  declarative and focus on the intent of a UI screen while deferring
  the concrete html/css/js representation. This allows for greater
  reuse and specialization of templates in different contexts. See
  https://github.com/tavisrudd/throw_out_your_templates/blob/master/throw_out_your_templates.py#L197
  for further discussion of this.

We intend to support client-side usage from clojurescript.

## Usage

FIXME: see the test suite for the time-being

## License

Copyright (C) 2012 Tavis Rudd and Roman Gonzalez

Distributed under the Eclipse Public License, the same as Clojure.
