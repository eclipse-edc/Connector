# AWS Test

To run AWS integration tests you will need a MinIO instance running:
```
docker run -d -p 9000:9000 -e MINIO_ROOT_USER=root -e MINIO_ROOT_PASSWORD=password bitnami/minio:latest
```

Then set the two environment variables:
```
S3_ACCESS_KEY_ID=root
S3_SECRET_ACCESS_KEY=password
```
