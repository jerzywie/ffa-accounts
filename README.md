# ffa-accounts

Clojurescript app to analyse FFA Account transactions

## Overview

It does the same processing as the command-line application 'ffa-account-analysis' but
renders the results in the browser.

## Development

To get an interactive development environment run:

    clojure -M:fig:build

This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    rm -rf target/public

To create a production build run:

	rm -rf target/public
	clojure -M:fig:min


## License

Copyright © 2021 Jerzywie

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
