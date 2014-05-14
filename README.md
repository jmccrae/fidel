FIDEL
=====

FIDEL Is a DEcoder and Language model. FIDEL is a small and compact
machine translation system, primarily meant for the educational
purposes.

Installation
------------

FIDEL can be installed with [Maven](http://maven.apache.org). The
easiest way to do this is with the following command

    mvn install

Alternatively you can manually obtain the following dependencies

* Apache Commons CLI 1.2
* FastUtil 6.4.4
* (Xerial) SQLite JDBC 3.7.2

Running
-------

FIDEL can be executed through Maven with the `fidel` command. Its usage
is as follows

    usage: fidel [opts]
    FIDEL: A Simple Decoder for Machine Translation
     -?         Display this message
     -b <arg>   The beam size (default=50)
     -f <arg>   The foreign (source) language
     -l <arg>   The language model
     -n <arg>   Output n-best translations
     -p <arg>   The phrase table
     -s         Output scores
     -t <arg>   The translation (target) language
     -v         Display debugging information
     -w <arg>   The weights file
     -z         Use lazy distortion
    
    example: fidel -p phrase-table.gz -l europarl.arpa.gz -w weights.cfg < data

FIDEL always reads from the STDIN and writes to STDOUT.

Requirements
------------

In order to run FIDEL you will need the following

## Phrase Table

A phrase table should be formatted as per Moses/Pharoah, that is of 
the form

_foreign phrase_ `|||` _translation phrase_ `|||` _score1_ _score2_ _score3_ _score4_ _score5_ `|||` _extra fields separated by_ `|||`

For example

    to go ||| gehen ||| 1.0 0.5 0.3 0.2 2.718 
    to go ||| fahren ||| 0.2 0.3 0.4 0.5 2.718

## Language Model

The language model should be in [ARPA Format](http://www.speech.sri.com/projects/srilm/manpages/ngram-format.5.html).

## Weights File

The weights should be one per line of the format:

_weight_`=`_value_

At least the following should be provided:

* `UnknownWord`: The unknown word penalty
* `LinearDistortion`: The penalty for distortion
* `LM`: The language model penalty
* `TM\:phi(t|f)`, `TM\:lex(t|f)`, `TM\:phi(f|t)`, `TM\:lex(f|t)`, `TM\:phrasePenalty`: The weights for the phrase table scores

Acknowledgments
---------------

FIDEL is licensed under the BSD license.

FIDEL was originally developed as part of the [Monnet Translation System](http://github.com/monnetproject/translation).
