# Standard Franz make rules forward to ant targets.

default:
	ant clean-build

clean:
	ant clean

prepush:
	ant prepush

build:
	ant build
