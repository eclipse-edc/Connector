{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "nifi.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "nifi.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
The name of the nifi headless service.
*/}}
{{- define "nifi.headless" -}}
{{- printf "%s-headless" (include "nifi.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create zookeeper.server
*/}}
{{- define "zookeeper.server" }}
{{- if .Values.zookeeper.enabled -}}
{{- printf "%s-zookeeper" .Release.Name }}
{{- else -}}
{{- printf "%s" .Values.zookeeper.server }}
{{- end -}}
{{- end -}}

{{/*
Create ca.server
*/}}
{{- define "ca.server" }}
{{- if .Values.nifi.tls.certificateSource.nifiToolkit -}}
{{- printf "%s-ca" .Release.Name }}
{{- else -}}
{{- printf "%s" .Values.ca.server }}
{{- end -}}
{{- end -}}

{{/*
Create the Zookeeper URL using user-provided zookeeper server-name and port
*/}}
{{- define "zookeeper.url" }}
{{- $port := .Values.zookeeper.port | toString }}
{{- if .Values.zookeeper.enabled -}}
{{- printf "%s-zookeeper:%s" .Release.Name $port }}
{{- else -}}
{{- printf "%s:%s" .Values.zookeeper.server $port }}
{{- end -}}
{{- end -}}
