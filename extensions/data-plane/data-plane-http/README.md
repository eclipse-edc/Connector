# Data Plane HTTP extension

This extension provides support for sending data sourced from an HTTP endpoint and posting data to an HTTP endpoint. By
nature of the DPF design, which supports _n_-way transfers, HTTP-sourced data can be sent to any `DataSink` type and an
HTTP endpoint can receive data from any `DataSource` type. The extension is designed to stream content to limit memory
consumption under load.

Note that Azure Object Storage or S3 extensions should be preferred to the current extensions when performing large data
transfers as support more scalable parallelization.