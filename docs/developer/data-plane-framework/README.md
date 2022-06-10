# Data Plane Framework

- [DPF Selector](dpf_selector.md)
- [DPF Transfer](dpf_transfer.md)

## Security

The Data Plane exposes a public HTTP endpoint root where all the data transfer calls pass. This could be used by malicious
attackers to attempt an attack e.g. DDoS.

By default, Jetty - that's the embedded HTTP server in the EDC - provides some defaults to avoid this:
- max request header size of 8 KB: every call that has the request header size larger than this this is discarded
- max form content size of 200 KB: every form request call that has a body larger than this value is discarded
- max form content keys of 1000: every form request call that has more keys than this value is discarded

No other limits are implemented, we strongly recommend putting a reverse proxy in front of the Data Plane (e.g. Nginx)
configured to block malicious calls.