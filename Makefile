.PHONY: dev

dev:
	clojure -A:dev -m figwheel.main --build devbuild --repl
