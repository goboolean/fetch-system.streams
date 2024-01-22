test-app:
	docker compose -p fetch-system-streams -f ./deploy/docker-compose.test.yml up --attach server --build --abort-on-container-exit
	docker compose -p fetch-system-streams -f ./deploy/docker-compose.test.yml down --remove-orphans

build-app:
	docker build -t registry.mulmuri.dev/fetch-system-streams:latest -f ./deploy/Dockerfile .
	docker push registry.mulmuri.dev/fetch-system-streams:latest

deploy-app:
	helm upgrade fetch-system ~/lab -n goboolean

sync-protobuf:
	@curl -s -L https://raw.githubusercontent.com/Goboolean/fetch-system.IaC/feature/model/api/protobuf/model.proto -o ./src/main/resources/model.proto

generate-protobuf: \
	sync-protobuf
	@protoc --java_out=./src/main/java ./src/main/resources/protobuf_model.proto