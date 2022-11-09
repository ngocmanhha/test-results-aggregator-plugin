all: tarball
	
tarball:
	git archive --format=tar HEAD | gzip > jenkins-in-house-plugins-test-results-aggregator-plugin.tar.gz

.PHONY: all tarball
