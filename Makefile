.PHONY: build

dev:
	yarn dev

release:
	yarn release

nrepl:
	clj -M:nrepl
