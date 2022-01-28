# E2E Test

## Run

Build docker image:
```
docker build -f launchers/e2e-test/Dockerfile -t ids-connector .
```

Run compose:
```
docker-compose -f launchers/e2e-test/docker-compose.yml up
```
