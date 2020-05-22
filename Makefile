.PHONY: build test

build:
	lein fig:build

production:
	lein fig:min
