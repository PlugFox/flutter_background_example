.PHONY: all build intall pigeon get

all: build

build:
	@flutter build apk --release

install:
	@flutter install

get:
	@(cd ./package/background && flutter pub get)
	@flutter pub get

pigeon: get
	@(cd ./package/background \
		&& flutter pub run pigeon \
		--input "pigeons/api.dart" \
		--dart_out "lib/src/controller/api.g.dart" \
		--kotlin_out "android/src/main/kotlin/tld/domain/controller/api/Api.kt" \
		--kotlin_package "tld.domain.controller.api")
